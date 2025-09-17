# JWT Authentication with Role-Based Access Control

A comprehensive **Spring Boot** authentication system implementing JWT tokens with **refresh token functionality** and **role-based authorization**.

---

## ✨ Features

- **JWT Authentication**: Secure token-based authentication with access and refresh tokens  
- **Role-Based Authorization**: Three-tier role system (`USER`, `MANAGER`, `ADMIN`) with hierarchical permissions  
- **Refresh Token**: Automatic token renewal with database storage and revocation capabilities  
- **Method-Level Security**: Fine-grained access control using Spring Security annotations  
- **User Management**: Complete CRUD operations with role-based restrictions  
- **Security Best Practices**: Separate signing keys, token expiration, and secure password encoding  

---

## 🛠 Tech Stack

- Spring Boot 3  
- Spring Security 6  
- JWT (`jsonwebtoken`)  
- JPA / Hibernate  
- BCrypt password encoding  
- Method-level security with `@PreAuthorize`  

---

## ⚙️ Key Components

- **Short-lived access tokens**: 15 minutes for API requests  
- **Long-lived refresh tokens**: 7 days, stored in database  
- **Role hierarchy**: `ADMIN > MANAGER > USER`  
- **Permission-based authorization**: Granular access control at method level  
- **Automatic token cleanup** and session management  

---

Perfect for applications requiring secure authentication with **multiple user privilege levels**.

