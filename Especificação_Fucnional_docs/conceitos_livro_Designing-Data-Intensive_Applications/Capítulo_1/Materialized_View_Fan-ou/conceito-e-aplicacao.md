# Materialized View & Fan-out — Cache Redis

---

## Parte 1 — O Conceito do Livro

### O problema que originou a ideia

O DDIA usa o Twitter como exemplo. Quando você abre o feed, o sistema precisa mostrar os posts de todas as pessoas que você segue. A pergunta é: **quando fazer esse trabalho — na leitura ou na escrita?**

**Abordagem 1 — trabalha na leitura (pull):**
Quando o usuário abre o feed, o sistema consulta o banco para buscar todos os posts de quem ele segue, ordena por data e retorna. Simples de implementar, mas muito caro: com milhões de usuários abrindo o feed ao mesmo tempo, isso gera milhões de queries pesadas por segundo.

**Abordagem 2 — trabalha na escrita (push / fan-out):**
Quando alguém posta, o sistema já entrega esse post na "caixa de entrada" de cada seguidor — armazenado num cache (Redis, por exemplo). Quando o usuário abre o feed, a leitura é trivial: só busca o que já está pronto.

O nome **fan-out** vem exatamente disso: um post se "espalha" para N caixas de entrada ao mesmo tempo, como um ventilador abrindo.

### O que é uma Materialized View

Uma **Materialized View** é o resultado pré-computado de uma query, armazenado como se fosse dado real. Em vez de calcular "quais são os vídeos mais assistidos" toda vez que alguém pede, você calcula uma vez e guarda o resultado pronto.

A consequência direta: **leitura vira apenas buscar o que já está calculado.** O custo vai para a escrita.

### O trade-off central

```
Trabalho na ESCRITA          Trabalho na LEITURA
────────────────────         ─────────────────────
Leitura muito rápida    vs   Leitura cara (query pesada)
Escrita mais complexa        Escrita simples
Cache pode ficar velho       Dado sempre fresco
```

Não existe certo ou errado — depende do padrão de uso. Se o sistema é lido 100 vezes para cada 1 escrita, vale a pena pagar o custo na escrita para que as 100 leituras sejam baratas.

### Stale data — o risco do cache

Cache tem TTL (time-to-live): após X segundos o dado expira e precisa ser recalculado. Enquanto não expira, o cache pode estar desatualizado. Isso é chamado de **stale data** (dado velho).

A escolha do TTL é um trade-off: TTL curto = dado mais fresco, mais recálculo. TTL longo = dado pode estar velho, menos recálculo.

---

## Parte 2 — Como Aplicar no VidaLongaFlix

### Por que faz sentido aqui

O VidaLongaFlix é um sistema de leitura intensiva. O catálogo de vídeos, as categorias, os vídeos mais assistidos — tudo isso é lido constantemente e muda raramente. Cada vez que alguém abre a home, o sistema faz as mesmas queries no banco de dados PostgreSQL. Com Redis, essas queries acontecem uma vez e o resultado fica pronto para todos.

### O que colocar em cache

| Dado | Endpoint atual | TTL sugerido |
|------|---------------|--------------|
| Lista de vídeos por categoria | `GET /videos` | 5 minutos |
| Vídeos mais assistidos | `GET /videos/most-viewed` | 2 minutos |
| Vídeos menos assistidos | `GET /videos/least-viewed` | 2 minutos |
| Lista de categorias | `GET /categories` | 10 minutos |
| Detalhes de um vídeo | `GET /videos/{id}` | 5 minutos |

### Como funciona o fluxo com cache

```
Usuário pede GET /videos/most-viewed
        │
        ▼
VideoService verifica Redis
        │
        ├── cache HIT → retorna imediatamente (< 1ms)
        │
        └── cache MISS → consulta PostgreSQL
                │
                └── salva resultado no Redis com TTL
                        │
                        └── retorna para o usuário
```

### Quando invalidar o cache

Quando alguém registra uma visualização (`PATCH /videos/{id}/view`):
- Atualiza o contador no PostgreSQL
- Invalida (apaga) a chave `most-viewed` no Redis

Na próxima leitura de most-viewed, o cache miss força recálculo com o dado fresco.

### O fan-out no contexto do VidaLongaFlix

Quando admin publica um novo vídeo, o sistema gera notificações para todos os usuários ativos. Isso é fan-out: 1 publicação → N notificações. Hoje isso acontece de forma síncrona — quando o sistema tiver muitos usuários, vai travar a request do admin. A solução natural conecta com o próximo conceito: fila assíncrona (SQS).

### Dependências para implementar

- Spring Data Redis (`spring-boot-starter-data-redis`)
- `@Cacheable`, `@CacheEvict` do Spring Cache
- Redis no docker-compose local + ElastiCache na AWS em produção
