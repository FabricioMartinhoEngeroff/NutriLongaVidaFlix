# Lost Update, Write Skew e Idempotência — Likes com Constraint Única

> DDIA — Capítulo 7 (Transactions): *lost update*, *write skew*, TOCTOU e o papel das *uniqueness constraints*.

---

## Parte 1 — O Conceito do Livro

### O problema que originou a ideia

Duas transações leem o mesmo dado, decidem algo com base nele e escrevem — ao mesmo tempo. Como cada uma decidiu olhando um estado que a outra já mudou, uma das escritas se perde ou o resultado fica inconsistente. O livro dá três nomes para variações desse problema:

**1. Lost update (atualização perdida)**

```
Contador de likes = 10

Requisição A            Requisição B
────────────            ────────────
lê 10
                        lê 10
soma 1 → 11
grava 11
                        soma 1 → 11
                        grava 11   ← o like da A sumiu

Resultado: 11 (deveria ser 12)
```

O padrão perigoso é o **read-modify-write** na aplicação: você lê um valor, calcula em memória e grava de volta. Entre o read e o write, outro processo mexeu no dado.

**2. Write skew (distorção de escrita)**

Duas transações leem o mesmo conjunto de linhas, cada uma escreve em uma linha *diferente*, e juntas violam uma regra que individualmente cada uma respeitava. Exemplo clássico: dois médicos de plantão pedem folga ao mesmo tempo; cada um vê "tem outro de plantão" e sai — resultado: ninguém de plantão.

**3. TOCTOU (time-of-check to time-of-use)**

Você *checa* uma condição ("esse like ainda não existe") e depois *age* com base nela ("então insiro"). Entre a checagem e a ação, o mundo mudou. É a forma mais comum de write skew no dia a dia.

### As soluções que o DDIA apresenta

| Solução | Como funciona | Quando usar |
|---|---|---|
| **Atomic write** (`UPDATE x = x + 1`) | O banco faz o read-modify-write dentro de uma operação atômica, com lock implícito na linha | Contadores, incrementos |
| **Explicit lock** (`SELECT ... FOR UPDATE`) | Trava a linha antes de ler; ninguém mexe até você commitar | Lógica que o banco não consegue expressar sozinho |
| **Compare-and-set** (`UPDATE ... WHERE valor = ?`) | Só grava se o valor ainda for o que você leu | Sistemas sem lock |
| **Uniqueness constraint** | O *banco* garante que não existem duas linhas iguais; a corrida vira um erro previsível, não um dado corrompido | Unicidade (username, 1 like por usuário) |
| **Serializable isolation** | O banco executa como se as transações fossem em série | Write skew que as opções acima não cobrem |

### O ponto central sobre uniqueness constraints

O livro é enfático: **garantia de unicidade é responsabilidade do banco, não da aplicação.** Se você "verifica se já existe e depois insere" em dois passos, sempre haverá uma janela de corrida. A constraint única fecha essa janela: no pior caso, a segunda escrita **falha com um erro** — e um erro previsível é infinitamente melhor que um dado duplicado silencioso.

---

## Parte 2 — Como Aplicar no VidaLongaFlix

### Onde isso vive no código

Likes são modelados como **linhas** na tabela `user_favorites`, com uma constraint única em `(user_id, item_id, item_type)`:

```sql
-- V7__create-table-user-favorites.sql
CONSTRAINT uk_user_item_type UNIQUE (user_id, item_id, item_type)
```

### O que já estava certo (o ponto mais difícil)

O `FavoriteService.countLikes` conta **linhas**, não incrementa uma coluna-contador:

```java
public long countLikes(String itemId, FavoriteContentType itemType) {
    return favoriteRepository.countByItemIdAndItemType(itemId, itemType); // COUNT(*)
}
```

Isso **elimina o lost update de raiz**. Não existe `UPDATE video SET likes = likes + 1` para dar corrida — cada like é uma linha independente, protegida pela constraint única. É exatamente o design que o DDIA recomenda para contagens que precisam ser exatas.

### O gap que corrigimos — TOCTOU no toggle

O `toggle()` fazia *ler → decidir → escrever* em passos separados:

```
Requisição A (curtir)      Requisição B (curtir, double-tap/retry)
─────────────────────      ──────────────────────────────────────
lê: não existe
                           lê: não existe
insere linha ✓
                           insere linha → viola uk_user_item_type
                           → DataIntegrityViolationException
                           → HTTP 500 para o usuário
```

A constraint *salvava os dados* (fica 1 linha só), mas o app **estourava 500**. A correção: confiar na constraint e tratar a corrida como idempotente.

**Antes:**
```java
@Transactional
public boolean toggle(UUID userId, String itemId, FavoriteContentType itemType) {
    ...
    favoriteRepository.save(favorite);  // estoura se a constraint bloquear
    return true;
}
```

**Depois** (`FavoriteService.toggle`):
```java
// SEM @Transactional de propósito: cada braço é UMA escrita atômica.
// Com @Transactional, a exceção marcaria a transação como rollback-only
// e o catch abaixo não teria efeito.
public boolean toggle(UUID userId, String itemId, FavoriteContentType itemType) {
    User user = findUser(userId);
    Optional<UserFavorite> existing =
            favoriteRepository.findByUser_IdAndItemIdAndItemType(userId, itemId, itemType);

    if (existing.isPresent()) {
        favoriteRepository.delete(existing.get()); // delete é idempotente (0 linhas = no-op)
        return false; // descurtido
    }

    try {
        favoriteRepository.save(UserFavorite.builder()
                .user(user).itemId(itemId).itemType(itemType).build());
        return true; // curtido
    } catch (DataIntegrityViolationException e) {
        // Corrida: outra requisição inseriu o mesmo like ao mesmo tempo.
        // A constraint garantiu 1 linha. Resultado correto = "já curtido".
        return true;
    }
}
```

### Detalhe fino — por que remover o `@Transactional`

Em Spring, quando uma `DataIntegrityViolationException` acontece dentro de uma transação, ela é marcada como **rollback-only**. Capturar a exceção e seguir em frente *dentro da mesma transação* não funciona — o commit final falharia mesmo assim. Como cada braço do toggle é uma única escrita (um `delete` OU um `insert`), atômica por si só, tirar o `@Transactional` é o que torna o `catch` eficaz.

### Teste que prova o conceito

`FavoriteServiceTest.shouldStayFavoritedWhenConcurrentLikeRacesOnUniqueConstraint`:

```java
given(favoriteRepository.save(any(UserFavorite.class)))
        .willThrow(new DataIntegrityViolationException("uk_user_item_type"));

boolean result = favoriteService.toggle(userId, "video-1", FavoriteContentType.VIDEO);

assertTrue(result); // corrida vira "curtido", não erro 500
```

### Regra prática para o projeto

- Contagem exata → conte **linhas** com constraint única, nunca uma coluna-contador incrementada na aplicação.
- Unicidade → deixe o **banco** garantir; trate a exceção da constraint de forma idempotente.
- Nunca "verifique e depois insira" como garantia de unicidade — isso é TOCTOU.

### Relacionados

- Ordenação determinística de comentários → `../../Capítulo_8_relógios_não_confiáveis/`
- Linearizability (a constraint única exige que todos os nós vejam o mesmo estado) → `../../Capítulo_9_consistência_e_consenso/linearizability/`
