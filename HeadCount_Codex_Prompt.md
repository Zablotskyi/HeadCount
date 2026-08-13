# MASTER PROMPT FOR CODEX — HEADCOUNT

## 1. Роль Codex

Ти — senior Java/Spring Boot developer, software architect, database designer, security engineer і frontend developer.

Твоє завдання — створити повноцінний production-ready вебдодаток **HeadCount** на базі наявного HTML/CSS/JavaScript-прототипу.

Працюй без хаотичного переписування всього проєкту. Спочатку проаналізуй поточний код і структуру файлів, після цього запропонуй короткий план, а потім реалізовуй систему поетапно.

Після кожного етапу:
1. проєкт має компілюватися;
2. застосунок має запускатися;
3. наявна функціональність не повинна безпідставно ламатися;
4. мають бути додані або оновлені тести;
5. коротко опиши створені та змінені файли;
6. вкажи команду для локального запуску і перевірки.

Не використовуй `localStorage` або `sessionStorage` як основне сховище бізнес-даних, користувачів, ролей, статусів HeadCount чи авторизації.

---

## 2. Наявні файли

У корені або у вихідній папці проєкту є чотири файли прототипу:

- `index.html`
- `index.js`
- `admin.html`
- `admin.js`

Спочатку обов’язково прочитай усі чотири файли та визнач:

- поточну структуру сторінок;
- логіку авторизації;
- логіку роботи з відділами;
- логіку роботи зі співробітниками;
- запуск HeadCount;
- зміну кольорів карток;
- підтвердження статусу;
- фільтрацію;
- можливості адміністратора;
- функціональність, яка зараз залежить від `localStorage` та `sessionStorage`.

Наявний frontend є прототипом. Його можна використати як візуальну і функціональну основу, але небезпечну клієнтську логіку потрібно перенести на backend.

---

## 3. Мета системи

HeadCount — вебсистема для побудови структури організації та перевірки безпеки співробітників під час тривоги, вибухів або іншої надзвичайної події.

Система повинна:

- відображати організаційну структуру;
- показувати всіх співробітників у вигляді карток;
- дозволяти відкривати та закривати рівні дерева організації;
- показувати короткі дані в картці;
- показувати повну анкету в модальному вікні або окремій панелі;
- підтримувати авторизацію;
- підтримувати ролі та права;
- дозволяти уповноваженим користувачам оголошувати HeadCount;
- дозволяти користувачам підтверджувати, що з ними все добре;
- дозволяти надсилати запит `Help`;
- показувати час і автора підтвердження;
- враховувати часові зони;
- підтримувати реєстрацію з модерацією адміністратором;
- мати повноцінну адміністративну панель;
- зберігати аудит важливих дій.

---

## 4. Технологічний стек

### Backend

Використовуй:

- Java 21;
- Spring Boot 3;
- Maven;
- Spring Web;
- Spring Security;
- Spring Data JPA;
- Hibernate;
- Jakarta Bean Validation;
- MySQL 8;
- Flyway;
- Spring Mail;
- OpenAPI / Swagger;
- JUnit 5;
- Mockito;
- Spring Boot Test;
- Testcontainers;
- Docker;
- Docker Compose.

MapStruct можна використовувати для DTO mapping.

Lombok використовуй лише помірно. Не приховуй за Lombok важливу доменну логіку.

### Frontend

Основний рекомендований frontend:

- Vue 3;
- Vite;
- Vue Router;
- Pinia;
- Fetch API або Axios;
- звичайний CSS або Bootstrap 5.

Наявний HTML/CSS/JS-прототип використай як основу дизайну та поведінки.

Не перенось небезпечні перевірки доступу у Vue. Frontend може приховати недоступну кнопку, але backend завжди повинен окремо перевіряти право на кожну операцію.

### Обмін даними

- REST API;
- JSON;
- Server-Sent Events для оновлення статусів активного HeadCount у реальному часі;
- WebSocket поки не використовувати без обґрунтованої необхідності.

---

## 5. Загальні архітектурні правила

Побудуй modular monolith.

Рекомендована структура пакетів:

```text
src/main/java/com/wasbyte/headcount
├── config
├── common
├── exception
├── security
├── auth
├── user
├── organization
├── headcount
├── registration
├── notification
├── audit
└── reporting
```

У кожному функціональному модулі можуть бути:

```text
controller
service
repository
entity
dto
mapper
validation
```

Не створюй одну глобальну папку для всіх controller або всіх service.

Дотримуйся принципів:

- separation of concerns;
- DTO замість прямої передачі JPA entity у REST;
- constructor injection;
- транзакції на service-рівні;
- централізована обробка помилок;
- зрозумілі назви класів і методів;
- відсутність бізнес-логіки у controller;
- відсутність секретів у Git;
- відсутність hardcoded email адміністратора;
- відсутність пароля, що дорівнює email.

---

## 6. Користувач і профіль

Не створюй окремі таблиці або Java-класи для кожного виду працівника.

Працівник, керівник, адміністратор, працівник безпеки та інші — це одна сутність `User`, що має:

- профіль;
- посаду;
- grade;
- ролі;
- місце в організаційній структурі;
- постійного line manager;
- тимчасові делегування повноважень.

Основні поля користувача:

- `id`;
- `resourceNumber`;
- `grade`;
- `firstName`;
- `lastName`;
- `mobileNumber`;
- `email`;
- `passwordHash`;
- `country`;
- `city`;
- `office`;
- `department`;
- `position`;
- `lineManager`;
- `address`;
- `authorizedPersonPhoneNumber`;
- `timeZone`;
- `status`;
- `enabled`;
- `emailVerified`;
- `createdAt`;
- `updatedAt`;
- `lastLoginAt`.

Вимоги:

- `resourceNumber` унікальний;
- `email` унікальний і case-insensitive;
- пароль ніколи не зберігається відкритим текстом;
- часову зону зберігати як IANA Zone ID, наприклад `Europe/Kyiv`;
- видалення користувача за замовчуванням має бути soft delete або archiving;
- історичні HeadCount-події не повинні втрачати дані через видалення користувача.

---

## 7. Ролі

Створи рольову модель, яка дозволяє одному користувачу мати декілька ролей.

Початковий перелік ролей:

```text
EMPLOYEE
COUNTRY_MANAGER
REGIONAL_MANAGER
SUPPORT_MANAGER
PROGRAM_MANAGER
DEPARTMENT_MANAGER
UNIT_MANAGER
SECURITY_OFFICER
SECURITY_MANAGER
ADMIN
```

Ролі повинні зберігатися в базі даних.

Не покладайся лише на роль для визначення підлеглих. Підлеглі визначаються через організаційну структуру, line manager та активні делегування.

Основні права:

### EMPLOYEE

- перегляд дозволеної структури;
- перегляд власного профілю;
- редагування лише дозволених власних полів, якщо це передбачено;
- підтвердження статусу лише за себе;
- надсилання `Help` лише за себе.

### DEPARTMENT_MANAGER / UNIT_MANAGER

- усе, що може EMPLOYEE;
- перегляд своїх підлеглих;
- підтвердження за себе;
- підтвердження за своїх прямих і дозволених непрямих підлеглих;
- автор підтвердження обов’язково фіксується.

### COUNTRY_MANAGER / REGIONAL_MANAGER

- перегляд відповідної частини структури;
- запуск HeadCount у дозволеній області;
- перегляд станів працівників у своїй області;
- підтвердження за підлеглих лише відповідно до політики доступу.

### SECURITY_MANAGER / SECURITY_OFFICER

- запуск HeadCount у дозволеній області відповідно до ролі;
- перегляд активної події;
- перегляд `Help`;
- робота зі статусами відповідно до дозволів.

### ADMIN

- повний доступ до довідників і структури;
- створення, редагування, архівування користувачів;
- керування ролями;
- побудова дерева організації;
- призначення line manager;
- створення тимчасових делегувань;
- оголошення HeadCount;
- зміна статусів учасників активного HeadCount;
- модерація реєстрацій;
- перегляд аудиту;
- закриття або скасування події.

Додай method-level security через `@PreAuthorize` або еквівалентний механізм.

---

## 8. Організаційна структура

Організація повинна бути деревом.

Основні рівні:

```text
ORGANIZATION
COUNTRY
REGION
OFFICE
DEPARTMENT
UNIT
```

Створи універсальну сутність `OrganizationUnit`:

- `id`;
- `name`;
- `code`;
- `type`;
- `parent`;
- `manager`;
- `active`;
- `sortOrder`;
- `createdAt`;
- `updatedAt`.

Вимоги:

- один вузол може мати дочірні вузли;
- не допускай циклів;
- не дозволяй зробити вузол власним предком;
- підтримуй переміщення вузла в дереві;
- підтримуй сортування;
- підтримуй архівування;
- видалення вузла з активними дочірніми вузлами або користувачами має бути заборонене або виконуватися лише через контрольований сценарій;
- backend повинен валідувати допустимі parent-child комбінації;
- frontend повинен дозволяти розгортати та згортати окремий вузол або все дерево.

Окремі довідники:

- `Position`;
- `Grade`;
- `City`, якщо потрібен керований довідник;
- типи посад;
- часові зони.

Адмін повинен мати CRUD для:

- країн;
- регіонів;
- офісів;
- відділів;
- підрозділів;
- посад;
- grade;
- користувачів;
- ролей у дозволених межах.

---

## 9. Керівники і тимчасове перепризначення

У користувача є постійний `lineManager`.

Зміна постійного керівника адміністратором повинна:

- проходити через backend;
- фіксуватися в аудиті;
- зберігати старе і нове значення;
- не створювати циклів у ланцюжку керівників.

Для відпустки або тимчасової відсутності не перезаписуй постійного керівника.

Створи `ManagerDelegation`:

- `id`;
- `originalManager`;
- `delegatedManager`;
- `scope`;
- `validFrom`;
- `validUntil`;
- `active`;
- `createdBy`;
- `createdAt`;
- `reason`.

Вимоги:

- перевіряти перетин активних делегувань;
- перевіряти часовий період;
- автоматично враховувати чинне делегування при перевірці прав;
- після завершення строку делегування права автоматично перестають діяти;
- усі створення, зміни та скасування делегувань аудіюються.

---

## 10. HeadCount event

Оголошення HeadCount є окремою подією.

Створи `HeadCountEvent`:

- `id`;
- `title`;
- `description`;
- `status`;
- `scopeOrganizationUnit`;
- `startedAt`;
- `startedBy`;
- `closedAt`;
- `closedBy`;
- `cancelledAt`;
- `cancelledBy`;
- `createdAt`.

Статуси події:

```text
ACTIVE
CLOSED
CANCELLED
```

Під час створення події:

1. визнач учасників відповідно до scope;
2. створи для кожного snapshot-запис участі;
3. збережи важливі snapshot-дані, щоб майбутня зміна структури не зруйнувала історичний звіт;
4. усім учасникам признач початковий статус `PENDING`;
5. на frontend їхні картки стають червоними.

Не дозволяй дублювати активну подію в тому самому scope без явно визначеної політики.

---

## 11. Статус учасника HeadCount

Створи `HeadCountResponse` або `HeadCountParticipant`.

Поля:

- `id`;
- `event`;
- `employee`;
- `employeeNameSnapshot`;
- `resourceNumberSnapshot`;
- `organizationPathSnapshot`;
- `status`;
- `confirmedAt`;
- `confirmedBy`;
- `confirmationSource`;
- `helpMessage`;
- `helpRequestedAt`;
- `updatedAt`;
- `version` для optimistic locking.

Статуси:

```text
PENDING
SAFE
NEED_HELP
```

Відображення:

- `PENDING` — червоний;
- `SAFE` — зелений;
- `NEED_HELP` — помаранчевий або інший контрастний колір;
- немає активної події — нейтральний стан.

Правила:

- користувач може підтвердити тільки себе;
- керівник може підтвердити себе та дозволених підлеглих;
- адміністратор може змінити статус будь-якого учасника;
- security-рівень працює відповідно до scope і permissions;
- backend повинен перевіряти право при кожному запиті;
- повторна зміна статусу має аудіюватися;
- при підтвердженні за іншу людину зберігай `confirmedBy`;
- показуй, хто саме підтвердив;
- кнопка `Help` повинна дозволяти додати коротке повідомлення;
- запит `Help` не можна непомітно перетворювати на `SAFE`;
- усі часові значення зберігай в UTC як `Instant`.

Приклад відображення:

```text
Все добре
12:43:17 (Europe/Kyiv)
Підтвердив: Іван Петренко
```

---

## 12. Часові зони

Правила роботи з часом:

- у backend і БД зберігати час у UTC;
- використовувати `Instant`;
- часову зону користувача зберігати як IANA Zone ID;
- frontend отримує UTC timestamp і назву часової зони;
- frontend форматує час для показу;
- біля часу обов’язково показувати часову зону в дужках;
- не використовувати серверну локальну часову зону як джерело істини.

---

## 13. Реєстрація і модерація

Створи форму самореєстрації.

Поля реєстрації повинні включати необхідні дані профілю. Чутливі адміністративні поля, ролі та остаточне місце в структурі можуть призначатися адміністратором.

Статуси користувача:

```text
PENDING_EMAIL_VERIFICATION
PENDING_APPROVAL
ACTIVE
REJECTED
SUSPENDED
ARCHIVED
```

Сценарій:

1. користувач заповнює форму;
2. backend валідує дані;
3. пароль хешується;
4. створюється заявка;
5. за потреби користувач підтверджує email;
6. адміну надсилається повідомлення;
7. посилання веде на сторінку заявки, але вимагає авторизації адміністратора;
8. адміністратор перевіряє і редагує дані;
9. призначає роль, position, grade, organization unit та line manager;
10. адміністратор активує або відхиляє заявку;
11. користувач отримує email про результат.

Не передавай у листі токен, який самостійно надає адміністративну дію без повторної перевірки доступу.

---

## 14. Авторизація і безпека

Використовуй Spring Security.

Основні вимоги:

- BCrypt або Argon2;
- secure HttpOnly cookie;
- SameSite policy;
- HTTPS-ready configuration;
- CSRF protection для cookie-based auth;
- CORS лише для конкретного frontend origin;
- session fixation protection;
- rate limiting або інший захист login endpoint;
- блокування або затримка після серії невдалих входів;
- password reset через одноразовий токен з обмеженим строком;
- токени зберігати у хешованому вигляді;
- не показувати, чи існує email, у password reset response;
- admin endpoints захищати на backend;
- не довіряти ролям або ID, переданим frontend;
- захист від IDOR;
- перевірка ownership і scope;
- DTO validation;
- escaping та захист від XSS;
- audit login success/failure, якщо це не створює витік секретів;
- секрети передавати через environment variables;
- не логувати паролі, токени та чутливі персональні дані.

Створи перший адміністративний обліковий запис без hardcoded password, наприклад через environment variables або окремий bootstrap-компонент, який працює лише при порожній базі.

---

## 15. Адмін-панель

Створи окремий admin layout.

Розділи:

### Dashboard

- активний HeadCount;
- загальна кількість учасників;
- кількість `PENDING`;
- кількість `SAFE`;
- кількість `NEED_HELP`;
- нові заявки на реєстрацію;
- останні адміністративні дії.

### Organization

- дерево організації;
- додавання вузла;
- редагування вузла;
- переміщення вузла;
- зміна керівника;
- архівування;
- згортання та розгортання;
- пошук;
- фільтри.

### Users

- список;
- пошук;
- pagination;
- фільтри;
- створення;
- редагування;
- активація;
- блокування;
- архівування;
- призначення ролей;
- призначення position;
- призначення grade;
- вибір organization unit;
- вибір line manager;
- тимчасове делегування;
- reset password action без показу пароля.

### Registrations

- заявки `PENDING_APPROVAL`;
- перегляд анкети;
- редагування;
- approve;
- reject;
- причина відмови.

### HeadCount

- створення події;
- вибір scope;
- активна подія;
- live-статистика;
- таблиця/картки учасників;
- фільтри `ALL`, `PENDING`, `SAFE`, `NEED_HELP`;
- ручна зміна статусу;
- перегляд Help;
- закриття;
- скасування;
- історія;
- експорт CSV.

### Dictionaries

- positions;
- grades;
- інші дозволені довідники.

### Audit

- actor;
- action;
- entity type;
- entity ID;
- old value;
- new value;
- timestamp;
- IP;
- user agent;
- фільтри.

Критичні видалення повинні мати confirmation dialog.

---

## 16. Основний frontend

Головна сторінка після входу:

- верхня панель із назвою HeadCount;
- поточний користувач;
- logout;
- інформація про активну подію;
- кнопка оголошення HeadCount лише для уповноважених ролей;
- дерево організації;
- картки співробітників;
- фільтри;
- live-оновлення;
- адаптивність для desktop, tablet і mobile.

Картка співробітника повинна показувати коротко:

- first name;
- last name;
- position;
- department або unit;
- resource number;
- mobile number, якщо користувач має право його бачити;
- поточний HeadCount status;
- час підтвердження;
- часову зону;
- автора підтвердження, якщо підтвердив інший користувач.

Повна анкета відкривається:

- по click;
- бажано не покладатися лише на hover;
- у modal або side panel;
- поля мають відображатися відповідно до permissions.

Функції дерева:

- `Expand all`;
- `Collapse all`;
- expand/collapse окремих вузлів;
- збереження UI-стану допустиме локально, але не бізнес-даних;
- пошук співробітника;
- фільтр за country, region, office, department;
- фільтр за HeadCount status.

---

## 17. REST API

Створи versioned API:

```text
/api/v1
```

Орієнтовні endpoint:

```text
POST   /api/v1/auth/login
POST   /api/v1/auth/logout
GET    /api/v1/auth/me
POST   /api/v1/auth/register
POST   /api/v1/auth/forgot-password
POST   /api/v1/auth/reset-password

GET    /api/v1/users
POST   /api/v1/users
GET    /api/v1/users/{id}
PUT    /api/v1/users/{id}
PATCH  /api/v1/users/{id}/status
PUT    /api/v1/users/{id}/manager
PUT    /api/v1/users/{id}/roles
POST   /api/v1/users/{id}/delegations
DELETE /api/v1/users/{id}/delegations/{delegationId}

GET    /api/v1/organization-units/tree
GET    /api/v1/organization-units
POST   /api/v1/organization-units
GET    /api/v1/organization-units/{id}
PUT    /api/v1/organization-units/{id}
POST   /api/v1/organization-units/{id}/move
DELETE /api/v1/organization-units/{id}

GET    /api/v1/positions
POST   /api/v1/positions
PUT    /api/v1/positions/{id}
DELETE /api/v1/positions/{id}

GET    /api/v1/grades
POST   /api/v1/grades
PUT    /api/v1/grades/{id}
DELETE /api/v1/grades/{id}

GET    /api/v1/headcounts
POST   /api/v1/headcounts
GET    /api/v1/headcounts/active
GET    /api/v1/headcounts/{id}
POST   /api/v1/headcounts/{id}/confirm-self
POST   /api/v1/headcounts/{id}/participants/{userId}/confirm
POST   /api/v1/headcounts/{id}/help
PATCH  /api/v1/headcounts/{id}/participants/{userId}/status
POST   /api/v1/headcounts/{id}/close
POST   /api/v1/headcounts/{id}/cancel
GET    /api/v1/headcounts/{id}/events
GET    /api/v1/headcounts/{id}/export

GET    /api/v1/admin/registrations
GET    /api/v1/admin/registrations/{id}
POST   /api/v1/admin/registrations/{id}/approve
POST   /api/v1/admin/registrations/{id}/reject

GET    /api/v1/audit
```

Це орієнтовний контракт. Перед реалізацією перевір цілісність дизайну і за потреби покращи URI, але не змінюй бізнес-вимоги.

Використовуй:

- pagination;
- sorting;
- filtering;
- коректні HTTP status codes;
- problem details або уніфікований error response;
- OpenAPI documentation;
- optimistic locking для конкурентних оновлень статусів.

---

## 18. Server-Sent Events

Додай SSE endpoint для активного HeadCount.

Приклади подій:

```text
HEADCOUNT_STARTED
PARTICIPANT_STATUS_CHANGED
HELP_REQUESTED
HEADCOUNT_CLOSED
HEADCOUNT_CANCELLED
```

Frontend після отримання події повинен оновлювати тільки потрібні дані, а не повністю перезавантажувати сторінку.

Передбач reconnect і періодичну синхронізацію стану після відновлення з’єднання.

SSE endpoint також повинен перевіряти авторизацію і scope.

---

## 19. База даних

Використовуй MySQL і Flyway.

Початкові таблиці:

```text
users
roles
user_roles
positions
grades
organization_units
manager_delegations
headcount_events
headcount_participants
registration_requests або users зі статусом pending
password_reset_tokens
email_verification_tokens
audit_logs
```

Додай:

- foreign keys;
- unique indexes;
- indexes для пошуку;
- indexes для активної HeadCount-події;
- indexes за status;
- version column для optimistic locking;
- created_at / updated_at;
- обмеження довжини текстових полів;
- міграції без `ddl-auto=create`.

Для development можна використовувати:

```properties
spring.jpa.hibernate.ddl-auto=validate
```

Схему створює Flyway.

---

## 20. Email notifications

Через Spring Mail реалізуй:

- повідомлення адміну про нову реєстрацію;
- повідомлення користувачу про активацію;
- повідомлення про відмову;
- password reset;
- email verification, якщо використовується;
- за потреби повідомлення про оголошення HeadCount.

Для development додай Mailpit у Docker Compose.

Email шаблони мають бути окремими файлами.

---

## 21. Аудит

Створи audit subsystem.

Аудіювати щонайменше:

- створення користувача;
- редагування користувача;
- зміна статусу користувача;
- призначення або зняття ролі;
- зміна line manager;
- створення, зміна і скасування delegation;
- зміна organization tree;
- запуск HeadCount;
- підтвердження за іншу людину;
- зміна статусу адміністратором;
- `Help`;
- закриття або скасування HeadCount;
- approve або reject реєстрації.

Не записуй у аудит пароль, password hash, reset token або інші секрети.

---

## 22. Тестування

Мінімально потрібні:

### Unit tests

- permission checks;
- subordinate resolution;
- manager delegation;
- organization cycle validation;
- HeadCount participant creation;
- confirmation rules;
- timezone conversion helper;
- user registration;
- approval flow.

### Integration tests

Через Testcontainers MySQL:

- repositories;
- Flyway migrations;
- login;
- protected endpoints;
- role checks;
- create HeadCount;
- confirm self;
- manager confirms subordinate;
- forbidden confirmation for unrelated user;
- Help flow;
- close event;
- registration approval.

### Frontend tests

- login form;
- organization tree;
- filters;
- employee card;
- confirmation;
- Help;
- role-dependent buttons;
- admin CRUD critical flows.

Не створюй тести, які лише підтверджують mock без перевірки бізнес-поведінки.

---

## 23. Docker і запуск

Створи:

- backend `Dockerfile`;
- frontend `Dockerfile`;
- `docker-compose.yml`;
- MySQL service;
- Mailpit service;
- environment example file;
- persistent volume;
- health checks.

Не додавай реальні секрети до `.env.example`.

Мають працювати команди:

```bash
docker compose up --build
```

і окремий development запуск backend/frontend.

Додай `README.md` з:

- prerequisites;
- local setup;
- environment variables;
- database migration;
- test commands;
- default ports;
- initial admin bootstrap;
- OpenAPI URL;
- Mailpit URL;
- production notes.

---

## 24. Git і якість коду

- не коміть secrets;
- створи `.gitignore`;
- використовуй зрозумілі atomic changes;
- не видаляй робочий код без пояснення;
- не залишай великі закоментовані блоки;
- не залишай `TODO` замість критичної функціональності;
- форматуй код;
- усувай compiler warnings, якщо вони вказують на проблему;
- не використовуй deprecated API;
- не створюй overengineering;
- не додавай мікросервіси;
- не додавай Kafka, Redis або Kubernetes без реальної потреби.

---

## 25. Етапи реалізації

Реалізовуй у такому порядку.

### Phase 0 — Analysis

- прочитай чотири наявні файли;
- опиши поточну функціональність;
- знайди небезпечні місця;
- запропонуй final project structure;
- не починай масове переписування до завершення аналізу.

### Phase 1 — Project foundation

- Spring Boot;
- Maven;
- MySQL;
- Flyway;
- Docker Compose;
- базова конфігурація;
- health endpoint;
- README.

### Phase 2 — Users and authentication

- User;
- Role;
- Position;
- Grade;
- Spring Security;
- login/logout/me;
- first admin bootstrap;
- тести.

### Phase 3 — Organization

- OrganizationUnit;
- tree API;
- CRUD;
- cycle validation;
- manager assignment;
- admin UI;
- тести.

### Phase 4 — Registration moderation

- registration;
- email notification;
- admin review;
- approve/reject;
- activation;
- тести.

### Phase 5 — HeadCount core

- events;
- participants;
- PENDING/SAFE/NEED_HELP;
- self-confirmation;
- manager confirmation;
- admin/security operations;
- audit;
- тести.

### Phase 6 — Frontend migration

- Vue 3;
- перенесення дизайну прототипу;
- login;
- organization tree;
- employee cards;
- profile modal;
- filters;
- role-based UI.

### Phase 7 — Realtime

- SSE;
- live status updates;
- reconnect;
- live counters.

### Phase 8 — Admin completion

- users;
- organization;
- roles;
- positions;
- grades;
- delegations;
- registration moderation;
- HeadCount history;
- audit;
- export.

### Phase 9 — Hardening

- security review;
- validation;
- error handling;
- concurrency;
- rate limiting;
- audit completeness;
- integration tests;
- production Docker configuration.

---

## 26. Перший результат, який потрібно надати

Після прочитання цього prompt і файлів проєкту не намагайся одразу реалізувати весь застосунок одним величезним комітом.

Спочатку надай:

1. аналіз чотирьох наявних файлів;
2. список функцій, які вже є;
3. список проблем прототипу;
4. запропоновану архітектуру;
5. запропоновану структуру каталогів;
6. модель основних таблиць;
7. план фаз;
8. перелік питань лише там, де без відповіді неможливо прийняти безпечне рішення.

Після цього почни **Phase 1**.

Якщо середовище Codex дозволяє редагувати файли, створи Phase 1 без додаткового очікування, використовуючи розумні значення за замовчуванням. Не вигадуй бізнес-правила, яких немає в цьому prompt. У сумнівному місці явно задокументуй припущення.

---

## 27. Definition of Done для MVP

MVP вважається готовим, коли:

- користувач може зареєструватися;
- адміністратор отримує заявку;
- адміністратор може активувати користувача;
- користувач може увійти;
- адміністратор може створити структуру;
- адміністратор може створювати й редагувати користувачів;
- користувачі відображаються картками;
- дерево можна згортати і розгортати;
- уповноважена роль може запустити HeadCount;
- усі учасники події переходять у `PENDING`;
- працівник може підтвердити себе;
- керівник може підтвердити дозволеного підлеглого;
- користувач може натиснути `Help`;
- видно час, часову зону та автора підтвердження;
- працюють фільтри `ALL`, `PENDING`, `SAFE`, `NEED_HELP`;
- адміністратор може змінити статус;
- подію можна закрити;
- статуси оновлюються в реальному часі;
- важливі дії аудіюються;
- backend і frontend запускаються через Docker Compose;
- основні бізнес-правила покриті тестами;
- README містить повну інструкцію запуску.

---

## 28. Важливі заборони

Не можна:

- зберігати користувачів у `localStorage`;
- зберігати HeadCount status у `localStorage`;
- перевіряти admin через фіксований email;
- використовувати пароль, що дорівнює email;
- довіряти frontend-перевіркам ролей;
- віддавати password hash через API;
- зберігати локальний час без UTC;
- фізично видаляти історичні дані HeadCount;
- змінювати статус за іншого користувача без запису автора;
- дозволяти керівнику підтверджувати стороннього працівника;
- створювати цикли в organization tree або manager chain;
- запускати застосунок з `ddl-auto=create` у normal environment;
- залишати API без validation і authorization;
- робити весь застосунок одним controller або service;
- переписувати frontend без збереження ключової логіки прототипу;
- заявляти про завершення, якщо тести або build не проходять.

---

## 29. Мова коду і документації

- назви класів, методів, змінних, endpoint та commit-style summaries — англійською;
- UI — українською;
- README можна написати українською;
- коментарі додавати лише там, де вони пояснюють неочевидну бізнес-логіку;
- не коментувати очевидний код.

Починай з аналізу наявних файлів і Phase 1.
