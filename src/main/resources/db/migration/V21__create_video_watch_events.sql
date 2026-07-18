-- =============================================================
-- V21 — Event Sourcing: tabela de eventos de visualização
--
-- CONCEITO: Em vez de guardar só o número "views = 47", gravamos
-- cada evento que aconteceu. Isso permite derivar qualquer métrica
-- futura sem precisar de nova coluna ou migration:
--   - views totais         → COUNT(*)
--   - views únicas         → COUNT(DISTINCT user_id)
--   - views por dia        → GROUP BY DATE(watched_at)
--   - histórico do usuário → WHERE user_id = :id
--
-- Eventos são IMUTÁVEIS: só inserimos, nunca editamos nem deletamos.
-- =============================================================

CREATE TABLE video_watch_events (

    -- UUID gerado pelo banco com gen_random_uuid().
    -- Mais eficiente que deixar o Java gerar e enviar: o banco
    -- já tem o valor pronto antes do INSERT terminar.
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),

    -- Qual vídeo foi assistido.
    -- ON DELETE CASCADE: se o vídeo for deletado pelo admin,
    -- todos os eventos desse vídeo somem junto. Sem isso ficariam
    -- registros "órfãos" apontando para um id que não existe mais.
    video_id    UUID NOT NULL REFERENCES videos(id) ON DELETE CASCADE,

    -- Quem assistiu — NULLABLE intencionalmente.
    -- O endpoint PATCH /videos/{id}/view é public (permitAll no SecurityConfig),
    -- então pode ser chamado sem token JWT. Nesses casos user_id fica null.
    -- ON DELETE SET NULL: se o usuário for deletado, o evento permanece
    -- (o histórico do vídeo não se perde), mas user_id vira null.
    user_id     UUID REFERENCES users(id) ON DELETE SET NULL,

    -- Quando o evento aconteceu. DEFAULT now() é o banco definindo o valor
    -- se o Java não enviar — mas nosso código sempre envia (Instant.now()).
    watched_at  TIMESTAMP NOT NULL DEFAULT now()
);

-- Índice composto em (video_id, watched_at):
-- serve para as duas queries mais comuns sobre um vídeo:
--   SELECT COUNT(*) FROM video_watch_events WHERE video_id = :id
--   SELECT * FROM video_watch_events WHERE video_id = :id AND watched_at > :data
-- O banco usa esse índice para ambas sem fazer full table scan.
CREATE INDEX idx_watch_events_video ON video_watch_events(video_id, watched_at);

-- Índice em (user_id, watched_at):
-- serve para queries de histórico de usuário:
--   SELECT video_id FROM video_watch_events WHERE user_id = :id ORDER BY watched_at DESC
-- Base para recomendação futura: "o que esse usuário assistiu recentemente?"
CREATE INDEX idx_watch_events_user  ON video_watch_events(user_id, watched_at);
