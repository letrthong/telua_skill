# Architectural Design & System Modeling Guide (`design/`)

This directory contains system design documents, architectural blueprints, Mermaid class diagrams, sequence flows, and API specs for `Java_Android` features.

---

## 📐 Standard System Design Template (`design/<feature_name>_design.md`)

Whenever designing a new feature or refactoring architecture, create a markdown file inside `design/` using the following standard template:

```markdown
# System Architecture Design: [Feature Name]

## 1. High-Level Architectural Overview
Explanation of the design pattern used (e.g., Clean Architecture, MVVM, Repository Pattern, Strategy + Observer).

## 2. Component Class Diagram (Mermaid)
```mermaid
classDiagram
    class UserViewModel {
        -UserRepository mRepository
        +fetchUserData(userId)
    }
    class UserRepository {
        <<interface>>
        +getUserById(userId) Optional~User~
    }
    class UserRepositoryImpl {
        -UserRemoteDataSource mRemoteDataSource
        +getUserById(userId) Optional~User~
    }
    UserViewModel --> UserRepository
    UserRepository <|.. UserRepositoryImpl
```

## 3. Asynchronous Sequence Flow (Mermaid)
```mermaid
sequenceDiagram
    participant UI as Activity/Fragment
    participant VM as UserViewModel
    participant Repo as UserRepository
    participant SDK as ThirdPartySdk
    UI->>VM: fetchUserData(id)
    VM->>Repo: getUserById(id)
    Repo->>SDK: fetchRemoteData() (Background Thread, max 5s timeout)
    SDK-->>Repo: RemoteResponse
    Repo-->>VM: Optional~User~
    VM-->>UI: LiveData/State Update on Main Thread
```

## 4. Data Models & API Contracts
Define immutable data holders (Java Records), DTOs, and interface method contracts.
```
