# Desacoplamento de Escrita — Fila Assíncrona (SQS)

---

## Parte 1 — O Conceito do Livro

### O problema que originou a ideia

Imagine que publicar um vídeo no sistema precisa fazer três coisas: salvar o vídeo no banco, enviar e-mail para 1.000 usuários e criar 1.000 notificações. Se tudo acontece dentro da mesma request HTTP, o admin fica esperando o servidor enviar 1.000 e-mails antes de receber a resposta. Isso é **acoplamento de escrita**: a operação principal fica presa esperando todos os efeitos colaterais terminarem.

### O que é desacoplamento de escrita

O DDIA mostra que sistemas resilientes e escaláveis separam **o que acontece agora** do **o que pode acontecer depois**. A solução é uma **fila de mensagens** (message queue):

1. A operação principal termina e coloca uma mensagem na fila
2. Retorna resposta imediatamente para quem chamou
3. Um worker (processo separado) lê a fila e processa a mensagem quando puder

```
Produtor (API)          Fila (SQS)          Consumidor (Worker)
──────────────          ──────────          ───────────────────
Publica vídeo   ──►  mensagem entra   ──►  envia e-mails
retorna 201 ✓                              cria notificações
                                           em paralelo, no ritmo certo
```

### Por que isso é importante segundo o DDIA

**Reliability:** se o serviço de e-mail cair, a mensagem fica na fila esperando. Quando o serviço voltar, o worker processa normalmente. Sem fila, a falha no e-mail quebraria a publicação do vídeo inteiro.

**Scalability:** o worker pode processar N mensagens em paralelo, independente do ritmo de chegada. Se chegarem 100 publicações ao mesmo tempo, a fila absorve o pico e o worker processa no seu ritmo.

**Decoupling:** produtor e consumidor não precisam existir ao mesmo tempo. O produtor só precisa saber que a fila existe — não precisa saber quem vai processar, quantos workers existem, ou quando vão processar.

### O trade-off central

```
COM FILA (assíncrono)              SEM FILA (síncrono)
─────────────────────              ───────────────────
Request retorna rápido        vs   Request espera tudo terminar
Notificação chega com delay        Notificação chega imediata
Falha no worker não quebra API     Falha no e-mail quebra a request
Mais infra para operar             Mais simples de depurar
```

O **delay** é o principal custo. Para notificações e e-mails, segundos de delay são aceitáveis. Para pagamentos ou confirmações críticas, talvez não.

### Garantias de entrega

Filas como SQS oferecem **at-least-once delivery**: a mensagem é entregue pelo menos uma vez, mas pode ser entregue mais de uma vez. Isso significa que o worker precisa ser **idempotente** — processar a mesma mensagem duas vezes não pode causar problema (ex: não enviar dois e-mails para o mesmo usuário pela mesma publicação).

---

## Parte 2 — Como Aplicar no VidaLongaFlix

### Por que faz sentido aqui

Hoje, quando o admin publica um vídeo, o `NotificationService` cria notificações para todos os usuários ativos de forma síncrona — dentro da mesma request. Com poucos usuários isso funciona. Com 500 usuários ativos, a request do admin vai travar. Com 5.000, vai dar timeout.

O mesmo vale para e-mails: boas-vindas, reset de senha, notificação de promoção na fila de espera — todos são enviados de forma síncrona hoje.

### O que vai para a fila

| Evento (Produtor) | Mensagem na fila | Ação do Worker (Consumidor) |
|---|---|---|
| Admin publica vídeo | `VideoPublished(videoId)` | Cria notificações + envia e-mails |
| Usuário se cadastra | `UserRegistered(userId)` | Envia e-mail de boas-vindas |
| Usuário promovido da fila | `UserPromoted(userId)` | Envia e-mail de ativação |
| Solicitação de reset de senha | `PasswordResetRequested(token)` | Envia e-mail com link |

### Como fica o fluxo atual vs com SQS

**Hoje (síncrono):**
```
POST /admin/videos
    │
    ├── salva vídeo no banco
    ├── para cada usuário ativo:
    │       ├── cria notificação no banco
    │       └── envia e-mail (SMTP) ← trava aqui se SMTP lento
    │
    └── retorna 201 (depois de tudo isso)
```

**Com SQS (assíncrono):**
```
POST /admin/videos
    │
    ├── salva vídeo no banco
    ├── publica mensagem VideoPublished na fila SQS
    └── retorna 201 imediatamente ✓

(segundos depois, em paralelo)
Worker lê VideoPublished
    ├── para cada usuário ativo:
    │       ├── cria notificação no banco
    │       └── envia e-mail
    └── confirma processamento (SQS deleta a mensagem)
```

### Idempotência — o detalhe que não pode ignorar

SQS pode entregar a mesma mensagem `VideoPublished(videoId)` duas vezes. O worker precisa verificar: "já processei essa publicação para esse usuário?" — antes de criar a notificação. Uma coluna `processed_at` na tabela ou uma chave no Redis resolvem isso.

### Dependências para implementar

- AWS SQS (já está na conta da AWS do projeto)
- Spring Cloud AWS (`io.awspring.cloud:spring-cloud-aws-starter-sqs`)
- `@SqsListener` no worker para consumir mensagens
- `SqsTemplate` no produtor para publicar mensagens
- Dead Letter Queue (DLQ): fila secundária que recebe mensagens que falharam N vezes — para não perder eventos
