# Relógios Não Confiáveis e Ordenação Determinística

> DDIA — Capítulo 8 (The Trouble with Distributed Systems): relógios não são confiáveis; Capítulo 9 (Consistency and Consensus): garantias de ordenação.

---

## Parte 1 — O Conceito do Livro

### O problema que originou a ideia

O Capítulo 8 é um capítulo de **diagnóstico** — ele existe para te deixar desconfortável com suposições que quase todo desenvolvedor faz sem perceber. Uma delas: *"posso confiar no timestamp para saber a ordem das coisas."*

Não pode. O livro mostra por quê:

- **Relógios de máquinas diferentes divergem.** Dois servidores nunca têm o relógio exatamente igual. O NTP corrige, mas com folga de dezenas a centenas de milissegundos — e às vezes o relógio *anda para trás* durante uma correção.
- **O relógio do cliente é ainda pior.** O celular do usuário pode estar com a hora errada, fuso trocado, ou simplesmente mentindo. Nunca use o horário enviado pelo cliente para ordenar nada.
- **"Depois" não é bem definido em sistemas distribuídos.** Se dois eventos acontecem com milissegundos de diferença em máquinas diferentes, o timestamp não diz com segurança qual veio primeiro.

### Time-of-day clock vs monotonic clock

O livro distingue dois tipos de relógio:

| Tipo | O que mede | Serve para ordenar eventos? |
|---|---|---|
| **Time-of-day clock** (`System.currentTimeMillis`, `LocalDateTime.now`) | "que horas são" — sincronizado por NTP, pode saltar | Não é confiável entre máquinas |
| **Monotonic clock** (`System.nanoTime`) | tempo decorrido, sempre cresce | Bom para *duração*, não para *timestamp absoluto* |

### A lição prática

Para exibir coisas em ordem, você precisa de um **valor de ordenação determinístico e gerado num ponto único de controle** — tipicamente o servidor, no momento em que grava. E precisa de um **critério de desempate**, porque dois eventos podem cair no mesmo instante e, sem desempate, a ordem fica indefinida (o Capítulo 9 chama isso de *total order* vs *partial order*: você quer uma ordem total, sem empates ambíguos).

---

## Parte 2 — Como Aplicar no VidaLongaFlix

### Onde isso vive no código

Comentários em vídeos. A entidade `Comment` grava a data **no servidor**, no momento da persistência:

```java
// Comment.java
@PrePersist
public void onCreate() {
    this.date = LocalDateTime.now(); // horário do servidor, não do cliente ✓
}
```

Isso já estava certo: o horário vem do servidor, não do payload enviado pelo frontend. Esse é o primeiro acerto — nunca deixamos o cliente ditar o `date`.

### O gap que corrigimos — faltava ordenar

O `getCommentsByVideo` buscava sem nenhuma cláusula de ordenação:

```java
// Antes — ordem arbitrária do banco
commentRepository.findByVideo_Id(videoId)
```

Sem `ORDER BY`, o banco devolve as linhas na ordem que quiser (depende do plano de execução, do storage, de vacuum...). O usuário via os comentários embaralhados.

**Depois** — ordenação determinística por valor do servidor, com desempate:

```java
// CommentRepository.java
// Ordena por date (valor do servidor) com desempate por id, para evitar
// ordem indefinida quando dois comentários caem no mesmo instante.
List<Comment> findByVideo_IdOrderByDateAscIdAsc(UUID videoId);
```

```java
// CommentService.getCommentsByVideo
return commentRepository.findByVideo_IdOrderByDateAscIdAsc(videoId).stream()
        .map(CommentResponseDTO::new)
        .toList();
```

### Por que o desempate por `id` importa

Dois usuários podem comentar no mesmo milissegundo. Ordenando só por `date`, esses dois comentários teriam ordem indefinida entre si — e ela poderia mudar a cada request, deixando a lista "pulando". Adicionar `id` como segundo critério dá uma **ordem total estável**: a lista sempre sai na mesma sequência.

### Regra prática para o projeto

- **Nunca** ordene por timestamp enviado pelo cliente. Gere o valor no servidor, no `@PrePersist` / no banco.
- Todo `ORDER BY` de listagem precisa de um **critério de desempate** (geralmente a PK) para dar ordem total estável.
- `LocalDateTime.now()` do servidor é aceitável para *este* app (single-node). Num sistema multi-nó, o próprio livro alerta que nem o relógio do servidor é confiável entre máquinas — aí a solução vira ID sequencial/lógico (ver Capítulo 9).

### Relacionados

- Idempotência e constraint única em likes → `../../Capítulo_7_transações/lost_update_e_idempotência_em_likes/`
- Ordem total e linearizability → `../../Capítulo_9_consistência_e_consenso/linearizability/`
