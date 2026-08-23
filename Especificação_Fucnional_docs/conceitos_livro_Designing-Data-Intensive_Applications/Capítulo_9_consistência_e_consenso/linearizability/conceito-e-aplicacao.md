# Linearizability — A Ilusão de uma Cópia Única

> DDIA — Capítulo 9 (Consistency and Consensus), pgs. ~402–412.

---

## Parte 1 — O Conceito do Livro

### A ideia central

Linearizability faz um sistema distribuído se comportar **como se existisse uma única cópia dos dados**. Depois que uma escrita completa, **toda** leitura seguinte — não importa de qual cliente ou de qual réplica — enxerga o valor novo. Sem leitura de réplica desatualizada, sem "às vezes vem o valor antigo".

**Analogia do livro (placar de esportes):** se você já viu o resultado do jogo, qualquer pessoa que perguntar depois de você também vê o mesmo resultado. Ninguém volta a ver o placar antigo.

**Regra formal:** existe um instante em que a escrita "vira" atômica. Antes dele, todos leem o valor velho; depois dele, todos leem o novo. O sistema parece ter um único registro global compartilhado.

### Linearizability ≠ Serializability

O livro insiste na diferença porque os nomes confundem:

| | Serializability (Cap. 7) | Linearizability (Cap. 9) |
|---|---|---|
| Sobre o quê | **Transações** com vários objetos | Operação em **um único objeto** |
| Garante o quê | Executam como se fossem em série | Respeita a ordem do **tempo real** |
| Noção de tempo | Não exige ordem de tempo real | Exige: se A terminou antes de B começar, B vê A |

As duas juntas formam **strict serializability** — o modelo mais forte que existe (usado no Google Spanner e no FoundationDB).

### Quando você REALMENTE precisa de linearizability

1. **Locks e eleição de líder.** Só um nó pode ser líder. Todos precisam concordar sobre quem tem o lock. ZooKeeper e etcd fazem isso.
2. **Constraints de unicidade.** Username único, um assento por reserva, saldo não-negativo, **um like por usuário**. Todos os nós precisam ver o mesmo estado antes de decidir "esse já existe".
3. **Cross-channel timing.** Você grava um dado num canal (storage) e avisa por outro canal (fila) que ele está pronto. Se o storage não for linearizável, quem recebe o aviso pode buscar o dado e **pegar a versão antiga** — porque a escrita ainda não propagou para a réplica que ele leu.

### Como se implementa (e o custo)

| Arquitetura | Linearizável? |
|---|---|
| Single-leader + replicação **síncrona** (lendo do líder) | Sim |
| Consenso (Raft, Zab — ZooKeeper, etcd) | Sim, por design |
| Single-leader + replicação **assíncrona** | Não (failover pode perder escritas) |
| Multi-leader / leaderless (estilo Dynamo, quórum) | Não por padrão |

O recado do livro: linearizability é a garantia **mais forte e mais intuitiva**, mas custa **performance e disponibilidade** (teorema CAP). Sistemas que a entregam corretamente são complexos por dentro — justamente para que você não precise se preocupar.

---

## Parte 2 — Como Aplicar no VidaLongaFlix

O VidaLongaFlix hoje roda com **PostgreSQL single-node**. Um banco relacional único é linearizável para operações numa linha — então você **herda** a garantia de graça, sem escrever código de consenso. O valor deste capítulo aqui é **saber nomear onde essa garantia é o que segura o sistema**, e onde ela vira problema se um dia escalar.

### Caso 1 — Constraint de unicidade em likes (JÁ dependemos disso)

A regra "um like por usuário por item" é uma constraint de unicidade — o caso 2 da lista acima. Ela só funciona porque o Postgres garante que **todas** as transações veem o mesmo estado antes de decidir se a linha já existe:

```sql
-- V7__create-table-user-favorites.sql
CONSTRAINT uk_user_item_type UNIQUE (user_id, item_id, item_type)
```

É isso que permite o toggle idempotente (ver `../../Capítulo_7_transações/lost_update_e_idempotência_em_likes/`). **Se um dia** os likes forem para um banco distribuído estilo Dynamo (multi-leader/leaderless), essa constraint **deixa de valer** — dois nós poderiam aceitar o mesmo like sem ver um ao outro. Aí seria preciso um serviço de consenso ou aceitar duplicatas e deduplicar depois.

### Caso 2 — Cross-channel timing no publish de vídeo (CORRIGIDO ✅)

Este é o exemplo do livro batendo direto no fluxo de vídeo do projeto. Era um risco real no código — abaixo o problema original e a correção aplicada.

**O problema (antes):** `VideoService.create()` publicava o evento *antes* do commit:

```java
@Transactional                                  // (1) abre a transação
public void create(VideoRequestDTO request) {
    ...
    saveVideo(video);                           // (2) grava — mas NÃO commitou ainda
    videoEventPublisher.publishVideoPublished(video); // (3) publica ANTES do commit
}                                               // (4) commit acontece só aqui, no fim do método
```

O commit da transação só acontece quando o método `@Transactional` **retorna** (passo 4). Mas a publicação já saiu no passo 3. Dois canais, ordem errada:

```
Canal A (banco)              Canal B (mensagem)
───────────────              ──────────────────
grava video (não commitado)
                             publica VideoPublished ──► worker lê a fila
                                                        consulta o banco pelo videoId
                                                        ← vídeo ainda não commitado! não acha
commit ✓ (tarde demais)
```

**Impacto por implementação do publisher (no cenário "antes"):**

| Publisher | Risco (antes da correção) |
|---|---|
| `SyncVideoEventPublisher` | Chamava o `NotificationService` na **mesma transação** → sem problema (tudo commitava junto) |
| `SqsVideoEventPublisher` + `VideoPublishedListener` | Mensagem ia para o SQS **antes do commit**. O worker podia consumir e consultar o banco antes do vídeo existir → *cross-channel timing* |

> **Regra do livro:** só publique no canal B **depois** que o canal A confirmou a escrita. Nunca antes, nunca em paralelo.

**A correção aplicada — `@TransactionalEventListener(AFTER_COMMIT)`**

Em vez de chamar o publisher inline, o `VideoService.create()` publica um **evento Spring interno** dentro da transação. Um dispatcher só repassa esse evento ao publisher **depois do commit**:

```java
// VideoService.create() — agora
saveVideo(video);
applicationEventPublisher.publishEvent(new VideoPublishedEvent(video)); // dentro da TX
```

```java
// VideoEventDispatcher — novo
@Transactional(propagation = Propagation.REQUIRES_NEW)   // (!) ver gotcha abaixo
@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
public void onVideoPublished(VideoPublishedEvent event) {
    videoEventPublisher.publishVideoPublished(event.video()); // só após o commit
}
```

Agora o fluxo é:

```
grava video ─► commit ✓ ─► dispatcher (AFTER_COMMIT) ─► publisher (SQS/notif.)
```

Se a transação der **rollback**, o listener **não dispara** — nenhuma mensagem/notificação fantasma para um vídeo que não existe. Vale para os dois publishers: o `Sync` cria as notificações após o commit; o `Sqs` envia a mensagem após o commit.

**O gotcha que descobrimos na prática (`REQUIRES_NEW`)**

Ao rodar os testes, o `NotificationFlowIntegrationTest` falhou: `unreadCount expected:<1> but was:<0>`. Motivo: no `AFTER_COMMIT` a transação original **já commitou**, mas seus recursos ainda estão presos à thread. Um método `@Transactional` (propagação padrão `REQUIRED`) chamado nessa fase **junta-se à transação já encerrada** e suas escritas são **silenciosamente descartadas** — a notificação era criada em memória e nunca commitava. A solução é forçar uma transação **nova** no dispatcher com `@Transactional(propagation = REQUIRES_NEW)`. Depois disso, o teste passou. É um erro clássico de quem usa event listeners transacionais — e agora está documentado aqui com a prova em teste.

**Alternativa para quando o volume crescer — Transactional Outbox**

O `AFTER_COMMIT` resolve o cross-channel timing, mas ainda há uma janela mínima: se o app cair *entre* o commit e o envio ao SQS, o evento se perde. Para garantia total (exactly-once na origem), o padrão é **Outbox**: gravar o evento numa tabela `outbox` na *mesma* transação do vídeo e ter um processo separado lendo a tabela e publicando no SQS. Fica registrado como evolução futura — o `AFTER_COMMIT` já cobre o caso do projeto hoje.

### Caso 3 — Onde NÃO precisamos de linearizability

- **Contagem de views** (`VideoWatchEvent`): consistência eventual é perfeitamente aceitável. Ninguém se importa se o número de views está 3 segundos atrasado. Não gaste linearizability (nem performance) aqui.
- **Contagem de likes/comentários exibida na UI**: idem — pode ter um pequeno atraso sem prejuízo.

O livro é claro: linearizability tem custo. Use nos pontos onde uma leitura velha causa **dano** (unicidade, dinheiro, locks) e evite onde só causa um número levemente desatualizado.

### Regra prática para o projeto

- Enquanto for **Postgres single-node**, você tem linearizability de linha de graça — a constraint única de likes depende disso.
- **Upload → fila**: publique a mensagem só *após* confirmar a gravação (evita o bug de cross-channel timing).
- **Views e contadores de UI**: consistência eventual basta; não force garantia forte onde não há dano.
- Se algum dado migrar para armazenamento distribuído, reavalie: constraints de unicidade e cross-channel timing são os primeiros a quebrar.

### Relacionados

- Idempotência e constraint única em likes → `../../Capítulo_7_transações/lost_update_e_idempotência_em_likes/`
- Ordenação determinística (ordem total) → `../../Capítulo_8_relógios_não_confiáveis/ordenação_por_valor_do_servidor/`
- Desacoplamento por fila (SQS, at-least-once, idempotência) → `../../Capítulo_1/desacoplamento_de_escrita/`