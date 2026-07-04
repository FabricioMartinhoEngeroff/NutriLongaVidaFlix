# Imutabilidade e Derivar State — Event Sourcing

---

## Parte 1 — O Conceito do Livro

### O problema com guardar só o estado atual

A maioria dos sistemas guarda o **estado atual** dos dados. Quando um usuário assiste um vídeo, incrementamos `views = views + 1`. Quando ele cancela um favorito, deletamos o registro. O banco sempre reflete "como as coisas estão agora".

O problema: **você perdeu a história**. Não sabe quantas vezes o mesmo usuário assistiu. Não sabe quando o favorito foi adicionado e quando foi removido. Não consegue responder "quantas visualizações esse vídeo teve na última semana?"

### O que é Event Sourcing

Em vez de guardar o estado atual, você guarda **cada evento que aconteceu**, em ordem, de forma imutável. O estado atual é sempre **derivado** desses eventos — calculado na hora ou armazenado como uma materialized view.

```
Abordagem tradicional:
  tabela videos → views: 47    ← só o número final

Event Sourcing:
  evento 1: VideoWatched(videoId=5, userId=12, at=2026-01-10 14:00)
  evento 2: VideoWatched(videoId=5, userId=88, at=2026-01-10 15:30)
  evento 3: VideoWatched(videoId=5, userId=12, at=2026-01-11 09:00)  ← mesmo usuário, segunda vez
  ...
  evento 47: VideoWatched(videoId=5, userId=203, at=2026-06-27 10:00)

  views totais = COUNT(eventos) = 47  ← derivado, não armazenado
  views únicas = COUNT(DISTINCT userId) = 46  ← derivado
  views esta semana = COUNT(WHERE at > ...)  ← derivado
```

### Por que imutabilidade importa

Eventos nunca são editados ou deletados — só adicionados. Isso tem consequências poderosas:

**Auditoria completa:** você sempre sabe o que aconteceu, quando, e quem causou. Impossível com estado mutável.

**Reprocessamento:** se você criar uma nova regra de negócio ou uma nova métrica, pode reprocessar todos os eventos do passado e derivar o resultado correto — como se a nova regra sempre tivesse existido.

**Debug:** quando um bug aparece, você tem o histórico completo do que aconteceu. Com estado mutável, o bug pode ter sobrescrito o dado — você nunca vai saber o que estava errado.

### O que é CQRS

DDIA apresenta **CQRS (Command Query Responsibility Segregation)** como o padrão natural que acompanha Event Sourcing:

- **Command (escrita):** grava um evento no log imutável
- **Query (leitura):** lê de uma materialized view otimizada para aquela query específica

```
Escrita                     Leitura
───────                     ───────
VideoWatched evento   ──►   views_counter (Redis ou tabela)
                      ──►   daily_views (tabela agregada por dia)
                      ──►   user_history (tabela por usuário)
```

Cada view de leitura pode ter a estrutura perfeita para a query que serve — sem compromisso com o modelo de escrita.

### O trade-off central

```
EVENT SOURCING                    ESTADO MUTÁVEL
──────────────                    ──────────────
Histórico completo           vs   Simples de implementar
Reprocessamento possível          Queries diretas no banco
Novas métricas sem migração       Sem storage extra de eventos
Audit log gratuito                Difícil auditar mudanças
Storage cresce com o tempo        Storage estável
```

---

## Parte 2 — Como Aplicar no VidaLongaFlix

### Por que faz sentido aqui

Hoje o `PATCH /videos/{id}/view` incrementa um contador simples. Você sabe **quantas** visualizações um vídeo tem — mas não **quem** assistiu, **quando**, nem **quantas vezes o mesmo usuário assistiu**. Essas informações têm valor real para um sistema de streaming: recomendar conteúdo, entender engajamento, identificar vídeos com alta retenção.

O melhor momento para começar é agora — a tabela de vídeos ainda é pequena, migrar eventos é barato.

### A abordagem gradual — começar junto ao contador

Não precisa jogar o contador fora. A estratégia é: **continuar atualizando `views`** para leitura rápida, **e também gravar o evento** para análise futura.

```
PATCH /videos/{id}/view
    │
    ├── incrementa videos.views (mantém o comportamento atual)
    └── insere VideoWatchedEvent(videoId, userId, timestamp) ← novo
```

Assim o sistema existente não quebra, e você começa a acumular histórico.

### Nova tabela de eventos (migration V21)

```sql
CREATE TABLE video_watch_events (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    video_id    UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    watched_at  TIMESTAMP NOT NULL DEFAULT now(),
    session_id  VARCHAR(64)  -- identifica sessão anônima se user_id for null
);

CREATE INDEX idx_watch_events_video ON video_watch_events(video_id, watched_at);
CREATE INDEX idx_watch_events_user  ON video_watch_events(user_id, watched_at);
```

### Queries que ficam possíveis com os eventos

```sql
-- Visualizações únicas (usuários distintos)
SELECT COUNT(DISTINCT user_id) FROM video_watch_events WHERE video_id = :id;

-- Visualizações por dia na última semana
SELECT DATE(watched_at), COUNT(*)
FROM video_watch_events
WHERE video_id = :id AND watched_at > now() - INTERVAL '7 days'
GROUP BY DATE(watched_at);

-- Usuários que assistiram mais de uma vez
SELECT user_id, COUNT(*) as vezes
FROM video_watch_events
WHERE video_id = :id
GROUP BY user_id HAVING COUNT(*) > 1;

-- Histórico do usuário (base para recomendação futura)
SELECT video_id, COUNT(*), MAX(watched_at)
FROM video_watch_events
WHERE user_id = :userId
GROUP BY video_id ORDER BY MAX(watched_at) DESC;
```

### O mesmo padrão para favoritos

O toggle de favorito hoje deleta o registro quando o usuário desfavorita. Com eventos:

```sql
CREATE TABLE favorite_events (
    id           UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id      UUID NOT NULL,
    item_id      UUID NOT NULL,
    content_type VARCHAR(20) NOT NULL,
    action       VARCHAR(10) NOT NULL,  -- 'ADDED' ou 'REMOVED'
    occurred_at  TIMESTAMP NOT NULL DEFAULT now()
);
```

Agora você sabe quando cada usuário adicionou e quando removeu — e pode derivar o estado atual (é o último evento para aquele par user/item).

### Caminho de evolução

```
Fase 1 (agora):    Gravar VideoWatchedEvent junto ao contador — sem quebrar nada
Fase 2 (futuro):   Novos endpoints de analytics usando os eventos
Fase 3 (futuro):   Recomendação baseada em histórico de eventos
Fase 4 (futuro):   CQRS completo — materialized views derivadas dos eventos
```

### Migrations necessárias

- `V21__create_video_watch_events.sql` — tabela de eventos de visualização
- `V22__create_favorite_events.sql` — tabela de eventos de favorito (opcional, fase posterior)
