# Índice Secundário — Busca Full-Text

---

## Parte 1 — O Conceito do Livro

### O que é um índice

Um banco de dados sem índice precisa varrer todas as linhas da tabela para encontrar o que você pediu — isso se chama **full table scan**. Com 100 linhas, não importa. Com 10 milhões de linhas, é catastrófico.

Um **índice** é uma estrutura de dados auxiliar que o banco mantém separada da tabela principal. Ele permite encontrar linhas rapidamente sem varrer tudo. O DDIA explica que índices são sempre uma **estrutura derivada**: criada a partir dos dados reais, não são os dados em si.

### O que é um índice primário vs secundário

**Índice primário:** criado automaticamente para a chave primária (`id`). Buscar `WHERE id = 'abc'` é sempre rápido — o banco sabe exatamente onde está essa linha.

**Índice secundário:** criado para outros campos além do id. `WHERE email = 'user@email.com'` ou `WHERE status = 'ACTIVE'` — sem índice secundário, o banco varre a tabela inteira.

No VidaLongaFlix você já usa índices secundários implicitamente: `findByEmail()`, `findByStatus()` — o Spring Data usa o índice no campo `email` e `status` da tabela `users`.

### O trade-off do índice

```
COM ÍNDICE                        SEM ÍNDICE
──────────                        ──────────
Leitura rápida (O log n)    vs    Leitura lenta (O n) — varre tudo
Escrita mais lenta                Escrita mais rápida
Ocupa espaço em disco             Menos espaço
```

Cada vez que você insere ou atualiza uma linha, o banco precisa atualizar todos os índices daquela tabela. Em tabelas com muitos índices e muitas escritas, o custo de manutenção dos índices pode ser significativo.

### O que é um índice de busca full-text

Um índice regular funciona por igualdade ou range: `email = 'x'` ou `created_at > '2025-01-01'`. Mas `WHERE description LIKE '%frango grelhado%'` não usa índice — é um full table scan com matching de string.

**Full-text search** é uma categoria especial de índice que tokeniza o texto (quebra em palavras), remove stopwords (palavras sem valor como "e", "de", "com"), aplica stemming (reduz palavras à raiz — "grelhou", "grelhar", "grelhado" viram "grelh") e cria um índice invertido: para cada palavra, guarda a lista de documentos que a contêm.

```
Texto original: "Frango grelhado com legumes frescos"
Tokens:         ["frango", "grelh", "legum", "fresc"]
Índice invertido:
  "frango" → [video_1, video_5, video_12]
  "grelh"  → [video_1, video_7]
  "legum"  → [video_1, video_3]
```

Buscar "frango grelhado" vira uma interseção de listas — muito mais rápido que varrer texto.

### PostgreSQL tsvector vs Elasticsearch

**PostgreSQL tsvector:** funcionalidade nativa do PostgreSQL. Sem novo serviço, sem nova infra. Suporta stemming em português, ranking por relevância, busca com operadores (AND, OR, NOT). Suficiente para a grande maioria dos casos.

**Elasticsearch:** sistema dedicado a busca. Muito mais poderoso: fuzzy matching (tolera erros de digitação), autocomplete, facets (filtros por categoria, duração), análise de linguagem mais sofisticada. Mas é uma peça de infra separada para operar, monitorar e sincronizar com o banco principal.

---

## Parte 2 — Como Aplicar no VidaLongaFlix

### Por que faz sentido aqui

Hoje não existe busca no VidaLongaFlix. O usuário só pode navegar por categoria. Quando o catálogo crescer — dezenas de receitas, vídeos de exercícios, cardápios variados — o usuário vai precisar digitar "frango proteico" ou "bolo sem açúcar" e encontrar o conteúdo relevante.

### O que indexar

| Tabela | Campos para busca |
|--------|------------------|
| `videos` | `title`, `description` |
| `menus` | `title`, `description`, `nutritionist_tips` |
| `categories` | `name` |

### Como o PostgreSQL full-text funciona na prática

O PostgreSQL usa dois tipos:
- `tsvector`: o documento indexado (resultado do processamento do texto)
- `tsquery`: a query de busca

```sql
-- Adicionar coluna de busca na tabela videos
ALTER TABLE videos ADD COLUMN search_vector tsvector;

-- Preencher com os campos relevantes (português)
UPDATE videos SET search_vector =
  to_tsvector('portuguese', coalesce(title,'') || ' ' || coalesce(description,''));

-- Criar índice GIN (otimizado para full-text)
CREATE INDEX videos_search_idx ON videos USING GIN(search_vector);

-- Buscar
SELECT * FROM videos
WHERE search_vector @@ plainto_tsquery('portuguese', 'frango grelhado')
ORDER BY ts_rank(search_vector, plainto_tsquery('portuguese', 'frango grelhado')) DESC;
```

### Como manter o índice atualizado

A coluna `search_vector` precisa ser atualizada quando o vídeo é criado ou editado. A forma mais robusta é via trigger no banco:

```sql
-- Trigger que atualiza search_vector automaticamente a cada insert/update
CREATE FUNCTION update_video_search() RETURNS trigger AS $$
BEGIN
  NEW.search_vector :=
    to_tsvector('portuguese', coalesce(NEW.title,'') || ' ' || coalesce(NEW.description,''));
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER videos_search_update
  BEFORE INSERT OR UPDATE ON videos
  FOR EACH ROW EXECUTE FUNCTION update_video_search();
```

Isso seria uma nova migration Flyway (`V21__add_fulltext_search.sql`).

### No Spring Boot

```java
// VideoRepository
@Query("SELECT v FROM Video v WHERE " +
       "to_tsvector('portuguese', v.title || ' ' || v.description) " +
       "@@ plainto_tsquery('portuguese', :query)")
List<Video> searchByText(@Param("query") String query);
```

### Caminho de evolução

```
Fase 1 (agora):     PostgreSQL tsvector — sem nova infra, resolve 90% dos casos
Fase 2 (futuro):    Elasticsearch — quando precisar de fuzzy search, autocomplete,
                    ou o catálogo crescer muito (> 10.000 itens)
```

### Nova migration necessária

`V21__add_fulltext_search.sql` — adiciona coluna `search_vector`, cria o trigger e o índice GIN nas tabelas `videos` e `menus`.
