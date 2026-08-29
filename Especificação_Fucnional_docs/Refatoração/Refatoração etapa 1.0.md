# Refatoração — Etapa 1.0

**Projeto:** VidaLongaFlix  
**Data:** Agosto de 2026  
**Escopo:** Serviços de autenticação, conteúdo e interação  
**Referências:** *Refactoring* (Martin Fowler, 2ª ed.) · *The Pragmatic Programmer* (Hunt & Thomas, 20th Anniversary Ed.)

---

## Contexto

Esta etapa iniciou o processo de limpeza estrutural do backend, aplicando técnicas clássicas de refatoração com foco em dois princípios centrais:

- **DRY** (*Don't Repeat Yourself*) — Dica 11 do Programador Pragmático: cada fragmento de conhecimento deve ter uma única representação no sistema.
- **Dois Chapéus** (Fowler, cap. 2): refatoração e adição de funcionalidade são trabalhos separados. Nesta etapa, apenas o chapéu de refatoração foi usado — nenhuma funcionalidade nova foi adicionada.

---

## O que foi feito, onde e por quê

### 1. Extração de `StringValidator` — *Código Duplicado*

**Cheiro detectado:** Código Duplicado (Fowler, cap. 3)  
**Refatoração aplicada:** Extrair Função → Classe Utilitária  
**Princípio PP:** DRY (Dica 11)

**Onde estava o problema:**

Os três serviços abaixo definiam, cada um, o mesmo método privado:

```java
// VideoService, CommentService e UserService — três cópias idênticas
private boolean isBlank(String field) {
    return field == null || field.isBlank();
}
```

Três representações do mesmo conhecimento. Se a regra de "campo vazio" mudasse, seria necessário alterar em três lugares — e esquecer um deles é um bug que nasce dormindo.

**Solução:**

Criada a classe `StringValidator` em `domain/shared/`:

```java
public final class StringValidator {
    public static boolean isBlank(String value)    { return value == null || value.isBlank(); }
    public static boolean isNotBlank(String value) { return !isBlank(value); }
}
```

Agora existe uma única representação. Os três serviços passaram a usar `StringValidator.isBlank()` e `StringValidator.isNotBlank()`.

**Arquivos alterados:**
- `services/content/VideoService.java`
- `services/interaction/CommentService.java`
- `services/auth/UserService.java`
- **Novo:** `domain/shared/StringValidator.java`

---

### 2. Extração de `ErrorMessages` — *Mensagens Hardcoded e Inconsistentes*

**Cheiro detectado:** Número Mágico / String Mágica + Nome Misterioso (Fowler, cap. 3)  
**Refatoração aplicada:** Substituir Número Mágico por Constante Simbólica  
**Princípio PP:** Glossário do Projeto (Dica 54) — uma única linguagem para os mesmos conceitos

**Onde estava o problema:**

Mensagens de erro espalhadas pelos serviços, com idiomas e formatos inconsistentes:

```java
// UserService
"Usuário não encontrado com ID: " + userId   // português

// CommentService
"User not found with id: " + userId           // inglês

// FavoriteService
"Usuário não encontrado"                       // sem o ID
```

Três representações do mesmo erro, com três formatos diferentes.

**Solução:**

Criada a classe `ErrorMessages` em `domain/shared/`:

```java
public final class ErrorMessages {
    public static String userNotFound(UUID id)     { return "User not found with id: " + id; }
    public static String videoNotFound(UUID id)    { return "Video with ID " + id + " not found."; }
    public static String categoryNotFound(UUID id) { return "Category with ID " + id + " not found."; }
    public static String commentNotFound(UUID id)  { return "Comment with ID " + id + " not found."; }
    public static String menuNotFound(UUID id)     { return "Menu with ID " + id + " not found."; }
}
```

**Arquivos alterados:**
- `services/content/VideoService.java`
- `services/interaction/CommentService.java`
- `services/interaction/FavoriteService.java`
- `services/auth/UserService.java`
- `services/auth/RegistrationLimitService.java`
- **Novo:** `domain/shared/ErrorMessages.java`

---

### 3. Mover Função — Autenticação saiu do Controller para o Service

**Cheiro detectado:** Inveja de Recursos (Fowler, cap. 3)  
**Refatoração aplicada:** Mover Função  
**Princípio PP:** Ortogonalidade (cap. 5) — controllers não devem saber como usuários são autenticados

**Onde estava o problema:**

O `AuthController` injetava diretamente `UserRepository` e `PasswordEncoder` para realizar a autenticação:

```java
// ANTES — lógica de negócio no controller
User user = userRepository.findByEmail(email)
    .orElseThrow(() -> new InvalidCredentialsException("..."));

if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
    throw new InvalidCredentialsException("...");
}
```

O controller sabia *como* autenticar — responsabilidade que pertence ao `UserService`. Isso é o cheiro de Inveja de Recursos: a classe está usando os recursos de outra para fazer um trabalho que não é dela.

**Solução:**

A lógica foi movida para `UserService.authenticate(email, password)`:

```java
// UserService
public User authenticate(String email, String rawPassword) {
    User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials"));

    if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
        throw new InvalidCredentialsException("Invalid credentials");
    }
    return user;
}
```

O `AuthController` passou a ter apenas três dependências: `UserService`, `TokenService` e `RegistrationLimitService`. `UserRepository` e `PasswordEncoder` foram removidos do controller.

**Arquivos alterados:**
- `controllers/auth/AuthController.java`
- `services/auth/UserService.java`

---

### 4. Extrair Função — `applyUpdates` no VideoService

**Cheiro detectado:** Função Longa (Fowler, cap. 3)  
**Refatoração aplicada:** Extrair Função

**Onde estava o problema:**

O método `update()` continha um bloco com mais de 10 condicionais `if (!isBlank(...))` embutidos diretamente no corpo do método, misturando a decisão de persistir com a lógica de quais campos atualizar.

**Solução:**

A lógica de atualização de campos foi extraída para o método privado `applyUpdates(video, request)`:

```java
private void applyUpdates(Video video, VideoRequestDTO request) {
    if (StringValidator.isNotBlank(request.title()))       video.setTitle(request.title());
    if (StringValidator.isNotBlank(request.description())) video.setDescription(request.description());
    // ...
}
```

O método `update()` ficou com uma única responsabilidade: encontrar o vídeo, aplicar as atualizações e salvar.

**Arquivo alterado:**
- `services/content/VideoService.java`

---

### 5. Substituir `System.err.println` por Logger — *Janela Quebrada*

**Cheiro detectado:** Janela Quebrada (Programador Pragmático, cap. 3 — Dica 4)  
**Princípio PP:** Não tolere janelas quebradas

**Onde estava o problema:**

`RegistrationLimitService` usava `System.err.println` para registrar falhas de notificação. Em produção, esse output vai para o stderr sem timestamp, sem nível de log, sem rastreabilidade.

**Solução:**

Substituído por `Logger` do SLF4J com nível `warn` e contexto estruturado:

```java
logger.warn("Welcome email not sent for user {}: {}", user.getEmail(), e.getMessage());
```

**Arquivo alterado:**
- `services/auth/RegistrationLimitService.java`

---

### 6. Constante `DEFAULT_MAX_ACTIVE_USERS` — *Número Mágico*

**Cheiro detectado:** Número Mágico / String Mágica  
**Refatoração aplicada:** Substituir por Constante

**Onde estava o problema:**

O valor `"100"` aparecia hardcoded em dois lugares dentro de `RegistrationLimitService`, sem deixar claro o que representava.

**Solução:**

```java
private static final String DEFAULT_MAX_ACTIVE_USERS = "100";
```

Uma única definição, com nome que documenta a intenção.

**Arquivo alterado:**
- `services/auth/RegistrationLimitService.java`

---

### 7. Remoção de Comentários Redundantes — *Comentários como Desodorante*

**Cheiro detectado:** Comentários (Fowler, cap. 3) — "comentário que só repete o que o nome já diz"  
**Princípio PP:** O código deve ser autoexplicativo; comentários explicam *por que*, não *o que*

Comentários como `// busca o usuário`, `// salva no banco` e blocos de `//` ornamentais foram removidos de `FavoriteService` e outros serviços. Quando o nome do método já diz tudo, o comentário é ruído.

---

### 8. Remoção de `NotificationService` — *Elemento Ocioso*

**Cheiro detectado:** Elemento Ocioso (Fowler, cap. 3)  
**Refatoração aplicada:** Colapsar Hierarquia / Remover Dependência Não Usada

O `VideoService` injetava `NotificationService` mas nunca o usava. A dependência foi removida do construtor.

**Arquivo alterado:**
- `services/content/VideoService.java`

---

## Testes ajustados e criados

| Arquivo | Motivo |
|---|---|
| `AuthControllerTest` | Mocks de `UserRepository` e `PasswordEncoder` removidos; adicionado mock de `UserService`. Stubs reescritos para usar `userService.authenticate()` |
| `StringValidatorTest` *(novo)* | Testes unitários para todos os casos de `isBlank` e `isNotBlank` |
| `ErrorMessagesTest` *(novo)* | Verifica que cada mensagem contém o UUID correto |

**Resultado:** 78 testes, 0 falhas.

---

## Princípios consolidados nesta etapa

| Conceito | Fonte | Onde foi aplicado |
|---|---|---|
| DRY | PP, Dica 11 | `StringValidator`, `ErrorMessages`, `DEFAULT_MAX_ACTIVE_USERS` |
| Extrair Função | Fowler, catálogo | `StringValidator`, `applyUpdates`, `getUserOrThrow` |
| Mover Função | Fowler, catálogo | Autenticação: controller → service |
| Inveja de Recursos | Fowler, cap. 3 | `AuthController` acessando `UserRepository` |
| Elemento Ocioso | Fowler, cap. 3 | `NotificationService` não usado |
| Janela Quebrada | PP, Dica 4 | `System.err.println` → Logger |
| Número Mágico | Fowler / PP | `"100"` → `DEFAULT_MAX_ACTIVE_USERS` |
| Comentários como Desodorante | Fowler, cap. 3 | Comentários redundantes removidos |
| Dois Chapéus | Fowler, cap. 2 | Nenhuma funcionalidade nova adicionada |
| Glossário do Projeto | PP, Dica 54 | Mensagens de erro unificadas em inglês |
