# Android Native Mobile REST API Specification (v1)

> **Audience**: Autonomous AI Engineer / Android Kotlin Developer building the native Android application with zero access to the backend codebase.  
> **Source of Truth**: Verified directly against Laravel 12 code (`routes/api.php`, controllers, resources, models, migrations, and middleware) on git branch `feature/mobile-api`.  
> **Git Commit Verification**: `46c1d8ab84cf694681997ae6e43d58e2b9143495` (`feat(api): mobile REST API v1 (auth, books, cart, checkout, orders, wishlist, reviews, profile)`).

---

## 1. Architectural Overview & Conventions

### Base URLs & Protocol
- **Production / Staging Base URL**: `https://<your-railway-app>.up.railway.app/api/v1`
- **Android Emulator Localhost**: `http://10.0.2.2:8000/api/v1`
- **Protocol**: HTTPS strictly enforced in production. All generated asset URLs (`cover_url`, `avatar_url`, `invoice_url`) use HTTPS in production.

### Global Request Headers
All requests from the Android application **must** supply:
```http
Accept: application/json
Content-Type: application/json
Accept-Language: en
```
*(For multipart endpoints like avatar upload, omit `Content-Type: application/json` so OkHttp/Retrofit automatically sets `multipart/form-data; boundary=...`).*

### Global Response Wrapper Convention
All JSON endpoints return one of two standard response structures:

#### Data Object or Collection:
```json
{
  "data": { ... }
}
```
or with a human-readable confirmation message:
```json
{
  "message": "Book added to cart successfully.",
  "data": { ... }
}
```

#### Paginated Collection:
```json
{
  "data": [ { ... } ],
  "links": {
    "first": "https://.../api/v1/books?page=1",
    "last": "https://.../api/v1/books?page=3",
    "prev": null,
    "next": "https://.../api/v1/books?page=2"
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 3,
    "links": [ ... ],
    "path": "https://.../api/v1/books",
    "per_page": 15,
    "to": 15,
    "total": 38
  }
}
```

### Standard Error Response Format
All error responses return a standardized JSON structure with HTTP status codes:

#### 422 Unprocessable Entity (Validation Errors)
```json
{
  "message": "The email has already been taken. (and 2 more errors)",
  "errors": {
    "email": [
      "The email has already been taken."
    ],
    "password": [
      "The password field confirmation does not match.",
      "The password field must be at least 8 characters."
    ]
  }
}
```

#### 401 Unauthorized (Missing or Invalid Token)
```json
{
  "message": "Unauthenticated."
}
```

#### 403 Forbidden (Unverified Email Gate)
```json
{
  "message": "Your email address is not verified.",
  "code": "email_not_verified"
}
```

#### 403 Forbidden (General / Unauthorized Access)
```json
{
  "message": "Forbidden."
}
```

#### 400 Bad Request (Already Authenticated on Guest Route)
```json
{
  "message": "Already authenticated."
}
```

#### 404 Not Found
```json
{
  "message": "Resource not found."
}
```

#### 429 Too Many Requests (Rate Limit Exceeded)
```json
{
  "message": "Too Many Requests"
}
```

#### 503 Service Unavailable (Payment Gateway Unconfigured)
```json
{
  "message": "Stripe payment gateway is not yet configured."
}
```

---

## 2. Authentication & Authorization

- **Token Type**: Bearer Token issued by Laravel Sanctum.
- **Header Format**: `Authorization: Bearer <token_string>`
- **Token Lifespan**: **Expires after 30 days** (`43,200` minutes). Once a token expires, subsequent requests return HTTP 401 `{"message": "Unauthenticated."}`, and the Android app must clear local credentials and navigate the user to the Login screen.
- **Guest Middleware**: Endpoints wrapped in `guest` (`register`, `login`, `forgot-password`, `reset-password`) return HTTP 400 `{"message": "Already authenticated."}` if called while an existing valid Bearer token is provided.
- **Verified Middleware**: Endpoints requiring email verification (`checkout`, `orders`, `profile/password`) return HTTP 403 with `code: "email_not_verified"` if the user has `email_verified_at == null`.

---

## 3. Rate Limits

Configured in `app/Providers/RouteServiceProvider.php` and enforced via `X-RateLimit-Limit` and `X-RateLimit-Remaining` headers:
- `api` (general fallback): **60 requests/minute**
- `api-read` (catalog browsing, categories, config): **120 requests/minute**
- `api-auth` (register, login, forgot/reset password): **10 requests/minute** per IP
- `api-coupon` (coupon apply/remove): **15 requests/minute** per user
- `api-checkout` (intent creation & confirm): **15 requests/minute** per user
- `api-review` (submitting reviews & voting helpful): **10 requests/minute** per user
- `auth/email/verification-notification`: **6 requests/minute**

---

## 4. API Endpoints Specification

### 4.1 System & Catalog (Public)

#### `GET /api/v1/config`
Retrieve application global settings and public credentials.
- **Auth**: Public (No auth required)
- **Rate Limit**: `api-read` (120/min)
- **Headers**: `Accept: application/json`
- **Path / Query Params**: None
- **Request Body**: None
- **Success Response (200 OK)**:
```json
{
  "data": {
    "app_name": "Laravel",
    "currency": "USD",
    "locales": [
      "en",
      "fr",
      "ar"
    ],
    "default_locale": "en",
    "stripe_publishable_key": "pk_test_...",
    "reservation_minutes": 15,
    "cover_placeholder_url": "https://.../images/covers/default.png"
  }
}
```
> [!IMPORTANT]
> Only `stripe_publishable_key` is ever exposed. Secret keys and webhook signing secrets are strictly hidden.

---

#### `GET /api/v1/categories`
List all active book categories with book counts.
- **Auth**: Public
- **Rate Limit**: `api-read` (120/min)
- **Headers**: `Accept: application/json`
- **Path / Query Params**: None
- **Request Body**: None
- **Success Response (200 OK)**:
```json
{
  "data": [
    {
      "id": 1,
      "name": "Fiction",
      "slug": "fiction",
      "books_count": 25
    },
    {
      "id": 2,
      "name": "Science & Technology",
      "slug": "science-tech",
      "books_count": 14
    }
  ]
}
```

---

#### `GET /api/v1/books`
Paginated catalog of active books.
- **Auth**: Public
- **Rate Limit**: `api-read` (120/min)
- **Headers**: `Accept: application/json`
- **Query Params**:
  - `page` (integer, optional, default: 1)
  - `category` (string or integer, optional): Category slug (e.g. `fiction`) or Category ID (e.g. `1`)
  - `search` (string, optional): Matches `title`, `author`, or `isbn`
  - `min_price` (numeric, optional, min: 0)
  - `max_price` (numeric, optional)
  - `in_stock` (boolean/integer, optional, `1` or `true` for items where `stock > 0`)
  - `sort` (string, optional, allowed values: `price_asc`, `price_desc`, `title_asc`, `title_desc`, `latest`; default: `latest`)
- **Success Response (200 OK)**:
```json
{
  "data": [
    {
      "id": 1,
      "title": "The Great Gatsby",
      "slug": "the-great-gatsby-demo",
      "author": "F. Scott Fitzgerald",
      "isbn": "9780743273565",
      "price": 14.99,
      "stock": 25,
      "in_stock": true,
      "cover_url": "https://.../storage/covers/gatsby.jpg",
      "has_sample": true,
      "category": {
        "id": 1,
        "name": "Fiction",
        "slug": "fiction"
      },
      "average_rating": 5,
      "reviews_count": 1,
      "created_at": "2026-11-23T10:05:10+00:00"
    }
  ],
  "links": {
    "first": "https://.../api/v1/books?page=1",
    "last": "https://.../api/v1/books?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "per_page": 15,
    "to": 1,
    "total": 1
  }
}
```

---

#### `GET /api/v1/books/{book}`
Retrieve comprehensive details for a single book.
- **Auth**: Public
- **Rate Limit**: `api-read` (120/min)
- **Path Params**: `{book}` can be the book's string `slug` (e.g. `the-great-gatsby-demo`) or its numeric `id` (e.g. `1`).
- **Success Response (200 OK)**:
```json
{
  "data": {
    "id": 1,
    "title": "The Great Gatsby",
    "slug": "the-great-gatsby-demo",
    "author": "F. Scott Fitzgerald",
    "description": "The story of the fabulously wealthy Jay Gatsby and his love for the beautiful Daisy Buchanan.",
    "sample_excerpt": "In my younger and more vulnerable years my father gave me some advice that I have been turning over in my mind ever since.",
    "reading_time_minutes": 1,
    "isbn": "9780743273565",
    "price": 14.99,
    "stock": 25,
    "in_stock": true,
    "cover_url": "https://.../storage/covers/gatsby.jpg",
    "has_sample": true,
    "category": {
      "id": 1,
      "name": "Fiction",
      "slug": "fiction"
    },
    "average_rating": 5,
    "reviews_count": 1,
    "rating_breakdown": [
      { "count": 1, "percentage": 100 },
      { "count": 0, "percentage": 0 },
      { "count": 0, "percentage": 0 },
      { "count": 0, "percentage": 0 },
      { "count": 0, "percentage": 0 }
    ],
    "related_books": [
      {
        "id": 2,
        "title": "1984",
        "slug": "1984-george-orwell",
        "price": 12.5,
        "cover_url": "https://.../storage/covers/1984.jpg",
        "in_stock": true
      }
    ],
    "created_at": "2026-11-23T10:05:10+00:00"
  }
}
```
- **Error Response (404 Not Found)**:
```json
{
  "message": "Resource not found."
}
```

---

#### `GET /api/v1/books/{book}/reviews`
Paginated reviews for a book (10 per page, newest first).
- **Auth**: Public
- **Path Params**: `{book}` (slug or numeric ID)
- **Success Response (200 OK)**:
```json
{
  "data": [
    {
      "id": 1,
      "user": {
        "id": 1,
        "name": "Jane Reader",
        "avatar_url": null
      },
      "rating": 5,
      "title": "An absolute classic!",
      "comment": "The prose is evocative and Fitzgerald captures the jazz age perfectly.",
      "is_verified_purchase": true,
      "helpful_count": 8,
      "created_at": "2026-11-23T10:05:11+00:00"
    }
  ],
  "links": {
    "first": "https://.../api/v1/books/the-great-gatsby-demo/reviews?page=1",
    "last": "https://.../api/v1/books/the-great-gatsby-demo/reviews?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "per_page": 10,
    "to": 1,
    "total": 1
  }
}
```

---

### 4.2 Authentication (`/api/v1/auth/...`)

#### `POST /api/v1/auth/register`
Register a new customer account and receive a personal access token.
- **Auth**: Guest only (no existing Bearer token)
- **Rate Limit**: `api-auth` (10/min)
- **Request Body**:
```json
{
  "name": "Alice Newbie",
  "email": "alice@example.com",
  "password": "SecurePass123!",
  "password_confirmation": "SecurePass123!",
  "device_name": "Google Pixel 9 (optional)"
}
```
- **Validation Rules**:
  - `name`: `required|string|max:255`
  - `email`: `required|string|lowercase|email|max:255|unique:users,email`
  - `password`: `required|string|confirmed|min:8`
  - `password_confirmation`: `required|string|same:password`
  - `device_name`: `nullable|string|max:255`
- **Success Response (201 Created)**:
```json
{
  "message": "Registered successfully.",
  "data": {
    "user": {
      "id": 3,
      "name": "Alice Newbie",
      "email": "alice@example.com",
      "email_verified": true,
      "email_verified_at": "2026-11-23T10:05:11+00:00",
      "is_admin": false,
      "avatar_url": null,
      "phone": null,
      "bio": null,
      "address_line1": null,
      "address_line2": null,
      "city": null,
      "state": null,
      "postal_code": null,
      "country": null,
      "created_at": "2026-11-23T10:05:11+00:00"
    },
    "token": "3|smEiWQiAQtRs411NDalTjHMBHWx90tp9N68wfc1Ka15fd007"
  }
}
```

---

#### `POST /api/v1/auth/login`
Authenticate existing customer and obtain a token.
- **Auth**: Guest only
- **Rate Limit**: `api-auth` (10/min) + lockout after 5 consecutive bad attempts.
- **Request Body**:
```json
{
  "email": "jane@example.com",
  "password": "Secret123!",
  "device_name": "Pixel 9 Pro (Android 15)"
}
```
- **Success Response (200 OK)**:
```json
{
  "message": "Login successful.",
  "data": {
    "user": {
      "id": 1,
      "name": "Jane Reader",
      "email": "jane@example.com",
      "email_verified": true,
      "email_verified_at": "2026-11-23T10:05:11+00:00",
      "is_admin": false,
      "avatar_url": null,
      "phone": "+1-555-0199",
      "bio": "Avid bibliophile and technology enthusiast.",
      "address_line1": "124 Oxford Street",
      "address_line2": "Apt 4B",
      "city": "London",
      "state": "Greater London",
      "postal_code": "W1D 1BS",
      "country": "United Kingdom",
      "created_at": "2026-11-23T10:05:11+00:00"
    },
    "token": "4|c3mK9zLtYhQ90XwF881pA9Kj9w8A1..."
  }
}
```
- **Error Response (401 Unauthorized)**:
```json
{
  "message": "The provided credentials do not match our records."
}
```

---

#### `POST /api/v1/auth/forgot-password`
Request a password reset link to user email.
- **Auth**: Guest only
- **Rate Limit**: `api-auth` (10/min)
- **Request Body**:
```json
{
  "email": "jane@example.com"
}
```
- **Success Response (200 OK)**:
```json
{
  "message": "If your email is registered in our records, a password reset link has been dispatched."
}
```

---

#### `POST /api/v1/auth/reset-password`
Complete password reset using email token.
- **Auth**: Guest only
- **Request Body**:
```json
{
  "token": "a1b2c3d4e5f6...",
  "email": "jane@example.com",
  "password": "NewSecret123!",
  "password_confirmation": "NewSecret123!"
}
```
- **Success Response (200 OK)**:
```json
{
  "message": "Password reset successfully."
}
```

---

#### `POST /api/v1/auth/logout`
Revoke the current Sanctum token and invalidate the session.
- **Auth**: `Bearer <token>`
- **Request Body**: None
- **Success Response (200 OK)**:
```json
{
  "message": "Logged out successfully."
}
```

---

#### `GET /api/v1/auth/user`
Fetch the currently authenticated user's record.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "data": {
    "id": 1,
    "name": "Jane Reader",
    "email": "jane@example.com",
    "email_verified": true,
    "email_verified_at": "2026-11-23T10:05:11+00:00",
    "is_admin": false,
    "avatar_url": null,
    "phone": "+1-555-0199",
    "bio": "Avid bibliophile and technology enthusiast.",
    "address_line1": "124 Oxford Street",
    "address_line2": "Apt 4B",
    "city": "London",
    "state": "Greater London",
    "postal_code": "W1D 1BS",
    "country": "United Kingdom",
    "created_at": "2026-11-23T10:05:11+00:00"
  }
}
```

---

#### `POST /api/v1/auth/email/verification-notification`
Resend verification email.
- **Auth**: `Bearer <token>`
- **Rate Limit**: 6/minute
- **Success Response (200 OK)**:
```json
{
  "message": "Verification email sent."
}
```
- **Already Verified (400 Bad Request)**:
```json
{
  "message": "Email is already verified."
}
```

---

### 4.3 Database-Backed Cart (`/api/v1/cart/...`)

Unlike the web version which relies on PHP session cookies, the mobile API utilizes an isolated server-side database cart table (`carts` and `cart_items`) tied directly to the authenticated user.

#### `GET /api/v1/cart`
Retrieve the authenticated customer's current shopping cart.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK - Empty Cart)**:
```json
{
  "data": {
    "items": [],
    "subtotal": 0,
    "discount": 0,
    "coupon_code": null,
    "total": 0,
    "items_count": 0
  }
}
```
- **Success Response (200 OK - Populated Cart with Applied Coupon)**:
```json
{
  "data": {
    "items": [
      {
        "book_id": 1,
        "title": "The Great Gatsby",
        "slug": "the-great-gatsby-demo",
        "price": 14.99,
        "quantity": 3,
        "subtotal": 44.97,
        "cover_url": "https://.../storage/covers/gatsby.jpg",
        "stock": 25,
        "in_stock": true
      },
      {
        "book_id": 2,
        "title": "1984",
        "slug": "1984-george-orwell",
        "price": 12.5,
        "quantity": 1,
        "subtotal": 12.5,
        "cover_url": "https://.../storage/covers/1984.jpg",
        "stock": 15,
        "in_stock": true
      }
    ],
    "subtotal": 57.47,
    "discount": 5.75,
    "coupon_code": "WELCOME10",
    "total": 51.72,
    "items_count": 4
  }
}
```

---

#### `POST /api/v1/cart/items`
Add an item to the shopping cart. Quantities accumulate if the item already exists in the cart.
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "book_id": 1,
  "quantity": 2
}
```
- **Validation**:
  - `book_id`: `required|integer|exists:books,id`
  - `quantity`: `required|integer|min:1`
- **Success Response (200 OK)**:
```json
{
  "message": "Book added to cart successfully.",
  "data": {
    "items": [
      {
        "book_id": 1,
        "title": "The Great Gatsby",
        "slug": "the-great-gatsby-demo",
        "price": 14.99,
        "quantity": 2,
        "subtotal": 29.98,
        "cover_url": "https://.../storage/covers/gatsby.jpg",
        "stock": 25,
        "in_stock": true
      }
    ],
    "subtotal": 29.98,
    "discount": 0,
    "coupon_code": null,
    "total": 29.98,
    "items_count": 2
  }
}
```
- **Error Response (422 Stock Exceeded)**:
```json
{
  "message": "Not enough stock available for \"The Great Gatsby\". Only 25 left (24 already in your bag)."
}
```

---

#### `PATCH /api/v1/cart/items/{book_id}`
Update the exact quantity of an item in the cart.
- **Auth**: `Bearer <token>`
- **Path Param**: `book_id` (integer)
- **Request Body**:
```json
{
  "quantity": 4
}
```
- **Success Response (200 OK)**:
```json
{
  "message": "Shopping bag updated.",
  "data": { ... }
}
```
- **Error Response (404 Not Found in Cart)**:
```json
{
  "message": "Item not found in cart."
}
```

---

#### `DELETE /api/v1/cart/items/{book_id}`
Remove a book entirely from the cart.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "message": "Volume removed from shopping bag.",
  "data": { ... }
}
```

---

#### `POST /api/v1/cart/clear`
Clear all items from the cart.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "message": "Shopping bag cleared.",
  "data": {
    "items": [],
    "subtotal": 0,
    "discount": 0,
    "coupon_code": null,
    "total": 0,
    "items_count": 0
  }
}
```

---

#### `POST /api/v1/cart/coupon`
Apply a discount coupon to the cart.
- **Auth**: `Bearer <token>`
- **Rate Limit**: `api-coupon` (15/min)
- **Request Body**:
```json
{
  "coupon_code": "WELCOME10"
}
```
- **Success Response (200 OK)**:
```json
{
  "message": "Coupon applied successfully.",
  "data": {
    "items": [ ... ],
    "subtotal": 57.47,
    "discount": 5.75,
    "coupon_code": "WELCOME10",
    "total": 51.72,
    "items_count": 4
  }
}
```
- **Error Response (422 Invalid / Expired)**:
```json
{
  "message": "Invalid coupon code."
}
```
or
```json
{
  "message": "Coupon is expired or has reached its usage limit."
}
```
or
```json
{
  "message": "Cart total must be at least $20.00 to use this coupon."
}
```

---

#### `DELETE /api/v1/cart/coupon`
Detach the applied coupon from the cart.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "message": "Coupon removed.",
  "data": {
    "items": [ ... ],
    "subtotal": 57.47,
    "discount": 0,
    "coupon_code": null,
    "total": 57.47,
    "items_count": 4
  }
}
```

---

### 4.4 Mobile Checkout & Orders (`/api/v1/checkout/...` & `/api/v1/orders/...`)

> [!IMPORTANT]
> All checkout and order endpoints strictly require **both** `auth:sanctum` and verified email (`verified.api`). Unverified accounts receive HTTP 403 `{"message":"Your email address is not verified.","code":"email_not_verified"}`.

#### `POST /api/v1/checkout/create-payment-intent`
Step 1 of Mobile Checkout: validates cart, places pessimistic 15-minute stock hold, creates a pending order with `source: mobile`, and initiates a Stripe `PaymentIntent`.
- **Auth**: `Bearer <token>` + Verified Email
- **Rate Limit**: `api-checkout` (15/min)
- **Request Body**:
```json
{
  "name": "Jane Reader",
  "address": "124 Oxford Street, Apt 4B",
  "city": "London",
  "postal_code": "W1D 1BS"
}
```
- **Validation Rules**:
  - `name`: `required|string|max:255`
  - `address`: `required|string|max:255`
  - `city`: `required|string|max:100`
  - `postal_code`: `required|string|max:50`
- **Behavior on Multiple Calls (Retries / Address Edits)**:
  - If invoked multiple times for the same cart (e.g. after editing shipping details or after a cancelled PaymentSheet), the backend **creates a new pending Order record** and generates a new Stripe `PaymentIntent` with a fresh `client_secret`.
  - Inventory stock is **NOT held twice**: `StockReservationService::reserveCart` performs an `updateOrCreate` on the reservation key `api_user_{userId}`, refreshing and extending the 15-minute expiration timestamp rather than stacking holds.
  - The previous unconfirmed pending orders simply stay abandoned in `pending` state and will never decrement stock.
  - **Client Action on Retry**: Discard the previous `order_id` and use the newly returned `order_id` and `client_secret` for `POST /checkout/confirm`.
- **Success Response (200 OK - Standard Paid Order)**:
```json
{
  "data": {
    "order_id": 4,
    "client_secret": "pi_3Qe94xLkd890123_secret_xyz789",
    "amount": 51.72,
    "currency": "USD",
    "free_order": false,
    "status": "pending"
  }
}
```
- **Success Response (200 OK - 100% Free Coupon Order)**:
When the total is $0.00, Stripe is skipped entirely. Stock is claimed and the order is immediately transitioned to `paid`. Cart is cleared.
```json
{
  "message": "Order placed successfully.",
  "data": {
    "order_id": 5,
    "client_secret": null,
    "amount": 0,
    "currency": "USD",
    "free_order": true,
    "status": "paid"
  }
}
```

---

#### `POST /api/v1/checkout/confirm`
Step 2 of Mobile Checkout: confirms payment after client-side Stripe PaymentSheet returns success.
- **Auth**: `Bearer <token>` + Verified Email
- **Rate Limit**: `api-checkout` (15/min)
- **Request Body Validation Rules**:
```json
{
  "order_id": 4,
  "payment_intent_id": "pi_3Qe94xLkd890123"
}
```
  - `order_id`: `required|integer|exists:orders,id`
  - `payment_intent_id`: `nullable|string`
- **Real Rule Regarding `payment_intent_id`**:
  `payment_intent_id` is defined as `nullable|string`. The controller fallback is:
  `$paymentIntentId = $validated['payment_intent_id'] ?? $order->stripe_payment_intent_id;`
  Because `POST /checkout/create-payment-intent` stores `stripe_payment_intent_id` on the order record, **the endpoint successfully confirms with `order_id` alone**.
  However, if the Android app supplies `payment_intent_id`, it is derived from `client_secret` (the substring before `_secret_`, e.g. `pi_3Qe94xLkd890123` from `pi_3Qe94xLkd890123_secret_xyz789`) or from `paymentIntent.id` in the Stripe SDK result.
- **Backend Flow & Business State**:
  - `confirm` operates on the already-persisted `Order` and `OrderItems` records created during `create-payment-intent`.
  - Any subsequent changes to the user's cart or coupons in another tab do **not** alter the order total, line items, or prices.
  - Stock is atomically decremented based on the order's snapshot items under pessimistic row-level locks.
  - Cart is cleared upon successful confirmation.
- **Real Responses**:
  - **Success (200 OK)**:
    ```json
    {
      "message": "Payment confirmed! Your order has been placed.",
      "data": {
        "id": 4,
        "status": "paid",
        "payment_status": "paid",
        "fulfillment_status": "unfulfilled",
        "total_amount": 51.72,
        "discount_amount": 5.75,
        "coupon_code": "WELCOME10",
        "customer_name": "Jane Reader",
        "customer_email": "jane@example.com",
        "shipping_address": "Jane Reader, 124 Oxford Street, Apt 4B, London W1D 1BS",
        "carrier": null,
        "tracking_number": null,
        "tracking_url": null,
        "estimated_delivery_at": null,
        "shipped_at": null,
        "delivered_at": null,
        "step_index": 1,
        "items": [
          {
            "id": 1,
            "book_id": 1,
            "title": "The Great Gatsby",
            "author": "F. Scott Fitzgerald",
            "quantity": 3,
            "price": 14.99,
            "book": {
              "slug": "the-great-gatsby-demo",
              "cover_url": "https://.../storage/covers/gatsby.jpg"
            }
          }
        ],
        "invoice_url": "https://.../api/v1/orders/4/invoice",
        "created_at": "2026-11-23T10:05:12.000000Z"
      }
    }
    ```
  - **Idempotent Retry (Order Already Paid) (200 OK)**:
    ```json
    {
      "message": "Order already confirmed.",
      "data": { ...OrderDetailResource... }
    }
    ```
  - **Order Not Found / Not Owned by User (404 Not Found)**:
    ```json
    {
      "message": "Order not found."
    }
    ```
  - **PaymentIntent Incomplete / Not Succeeded (422 Unprocessable Entity)**:
    ```json
    {
      "message": "Payment has not succeeded yet (status: requires_payment_method)."
    }
    ```
  - **Missing Payment Intent Reference (422 Unprocessable Entity)**:
    ```json
    {
      "message": "Missing payment intent reference."
    }
    ```

---

#### `GET /api/v1/orders`
Paginated order history for the authenticated customer (10 per page, latest first).
- **Auth**: `Bearer <token>` + Verified Email
- **Success Response (200 OK)**:
```json
{
  "data": [
    {
      "id": 1,
      "status": "shipped",
      "payment_status": "paid",
      "fulfillment_status": "shipped",
      "total_amount": 27.49,
      "discount_amount": 0,
      "coupon_code": null,
      "items_count": 2,
      "tracking_number": "DHL-89234710",
      "carrier": "DHL Express",
      "tracking_url": "https://www.dhl.com/en/express/tracking.html?AWB=DHL-89234710",
      "step_index": 3,
      "created_at": "2026-11-23T10:05:11.000000Z"
    }
  ],
  "links": {
    "first": "https://.../api/v1/orders?page=1",
    "last": "https://.../api/v1/orders?page=1",
    "prev": null,
    "next": null
  },
  "meta": {
    "current_page": 1,
    "from": 1,
    "last_page": 1,
    "per_page": 10,
    "to": 1,
    "total": 1
  }
}
```

---

#### `GET /api/v1/orders/{id}`
Full details for a specific order owned by the authenticated customer.
- **Auth**: `Bearer <token>` + Verified Email
- **Success Response (200 OK)**:
```json
{
  "data": {
    "id": 1,
    "status": "shipped",
    "payment_status": "paid",
    "fulfillment_status": "shipped",
    "total_amount": 27.49,
    "discount_amount": 0,
    "coupon_code": null,
    "customer_name": "Jane Reader",
    "customer_email": "jane@example.com",
    "shipping_address": "Jane Reader, 124 Oxford Street, Apt 4B, London W1D 1BS",
    "carrier": "DHL Express",
    "tracking_number": "DHL-89234710",
    "tracking_url": "https://www.dhl.com/en/express/tracking.html?AWB=DHL-89234710",
    "estimated_delivery_at": "2026-11-25",
    "shipped_at": "2026-11-22T10:05:11.000000Z",
    "delivered_at": null,
    "step_index": 3,
    "items": [
      {
        "id": 1,
        "book_id": 1,
        "title": "The Great Gatsby",
        "author": "F. Scott Fitzgerald",
        "quantity": 1,
        "price": 14.99,
        "book": {
          "slug": "the-great-gatsby-demo",
          "cover_url": "https://.../storage/covers/gatsby.jpg"
        }
      },
      {
        "id": 2,
        "book_id": 2,
        "title": "1984",
        "author": "George Orwell",
        "quantity": 1,
        "price": 12.5,
        "book": {
          "slug": "1984-george-orwell",
          "cover_url": "https://.../storage/covers/1984.jpg"
        }
      }
    ],
    "invoice_url": "https://.../api/v1/orders/1/invoice",
    "created_at": "2026-11-23T10:05:11.000000Z"
  }
}
```
- **Error Response (404 Not Found)**: Returns 404 if the order does not exist or belongs to another user.

---

#### `GET /api/v1/orders/{id}/invoice`
Download the official PDF tax invoice.
- **Auth**: `Bearer <token>` + Verified Email (ownership enforced)
- **Headers Verified**:
  - `Accept: application/pdf` -> Returns HTTP 200, `Content-Type: application/pdf`, `Content-Disposition: attachment; filename=invoice-ORD-00001.pdf`
  - `Accept: */*` -> Returns HTTP 200, `Content-Type: application/pdf`
  - `Accept: application/json` -> Returns HTTP 200, `Content-Type: application/pdf`
- **Technical Note on Middleware**: Even though the `ForceJsonResponse` middleware sets incoming request `Accept` to JSON, DomPDF's `$pdf->download()` explicitly sets the outgoing response header to `Content-Type: application/pdf`. **It does not break PDF download.**
- **Error Behavior**: If an error occurs (e.g. 401 unauthenticated, 403 forbidden, 404 not found), the server returns HTTP 4xx with JSON `{"message": "..."}`.
- **Client Implementation Recommendation**:
  Send `Authorization: Bearer <token>` and `Accept: application/pdf`. Verify `response.isSuccessful` (status 200) before streaming bytes to disk. If `!isSuccessful`, parse response body as JSON error.

---

### 4.5 Wishlist (`/api/v1/wishlist/...`)

#### `GET /api/v1/wishlist`
List all books in the authenticated customer's wishlist.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "data": [
    {
      "id": 3,
      "title": "Clean Architecture",
      "slug": "clean-architecture",
      "author": "Robert C. Martin",
      "isbn": "9780134494166",
      "price": 34.99,
      "stock": 8,
      "in_stock": true,
      "cover_url": "https://.../storage/covers/clean-arch.jpg",
      "has_sample": true,
      "category": {
        "id": 2,
        "name": "Science & Technology",
        "slug": "science-tech"
      },
      "average_rating": 0,
      "reviews_count": 0,
      "created_at": "2026-11-23T10:05:10+00:00"
    }
  ]
}
```

---

#### `POST /api/v1/wishlist/toggle`
Toggle a book's saved status (adds if missing, removes if present).
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "book_id": 1
}
```
- **Success Response (200 OK - Added)**:
```json
{
  "message": "Added \"The Great Gatsby\" to wishlist.",
  "data": {
    "book_id": 1,
    "is_wishlisted": true,
    "wishlist_count": 2
  }
}
```
- **Success Response (200 OK - Removed)**:
```json
{
  "message": "Removed \"The Great Gatsby\" from wishlist.",
  "data": {
    "book_id": 1,
    "is_wishlisted": false,
    "wishlist_count": 1
  }
}
```

---

#### `DELETE /api/v1/wishlist/{book_id}`
Remove a book from wishlist.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "message": "Volume removed from your saved items.",
  "data": {
    "wishlist_count": 1
  }
}
```

---

#### `POST /api/v1/wishlist/{book_id}/move-to-cart`
Remove a book from the wishlist and add 1 quantity to the database cart.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "message": "Volume moved to shopping bag.",
  "data": {
    "items": [
      {
        "book_id": 3,
        "title": "Clean Architecture",
        "slug": "clean-architecture",
        "price": 34.99,
        "quantity": 1,
        "subtotal": 34.99,
        "cover_url": "https://.../storage/covers/clean-arch.jpg",
        "stock": 8,
        "in_stock": true
      }
    ],
    "subtotal": 34.99,
    "discount": 0,
    "coupon_code": null,
    "total": 34.99,
    "items_count": 1
  }
}
```

---

### 4.6 Reviews (`/api/v1/...`)

#### `POST /api/v1/books/{book}/reviews`
Submit a new review or update an existing review for a book.
- **Auth**: `Bearer <token>`
- **Rate Limit**: `api-review` (10/min)
- **Path Param**: `{book}` (slug or ID)
- **Request Body**:
```json
{
  "rating": 5,
  "title": "Chilling dystopia that still resonates",
  "comment": "Orwells vision of totalitarian surveillance is as chilling and relevant today as ever."
}
```
- **Validation**:
  - `rating`: `required|integer|min:1|max:5`
  - `title`: `nullable|string|max:150`
  - `comment`: `required|string|min:5|max:2000`
- **Business Rule**: The backend inspects customer order history via `$user->hasPurchased($book)`. If the user purchased the book, `is_verified_purchase` is automatically set to `true`.
- **Success Response (200 OK)**:
```json
{
  "message": "Thank you! Your verified purchase review has been published.",
  "data": {
    "id": 2,
    "user": {
      "id": 1,
      "name": "Jane Reader",
      "avatar_url": null
    },
    "rating": 5,
    "title": "Chilling dystopia that still resonates",
    "comment": "Orwells vision of totalitarian surveillance is as chilling and relevant today as ever.",
    "is_verified_purchase": true,
    "helpful_count": 0,
    "created_at": "2026-11-23T10:05:12+00:00"
  }
}
```

---

#### `POST /api/v1/reviews/{id}/helpful`
Mark a review as helpful.
- **Auth**: `Bearer <token>`
- **Rate Limit**: `api-review` (10/min)
- **Business Rule**: Deduplicated per customer in database table `review_helpful_votes`.
- **Success Response (200 OK)**:
```json
{
  "message": "Thank you for your feedback.",
  "data": {
    "helpful_count": 9
  }
}
```
- **Error Response (422 Already Voted)**:
```json
{
  "message": "You have already marked this review as helpful."
}
```

---

### 4.7 Profile Management (`/api/v1/profile/...`)

#### `GET /api/v1/profile`
Get current user profile data.
- **Auth**: `Bearer <token>`
- **Success Response (200 OK)**:
```json
{
  "data": {
    "id": 1,
    "name": "Jane Reader",
    "email": "jane@example.com",
    "email_verified": true,
    "email_verified_at": "2026-11-23T10:05:11+00:00",
    "is_admin": false,
    "avatar_url": null,
    "phone": "+1-555-0199",
    "bio": "Avid bibliophile and technology enthusiast.",
    "address_line1": "124 Oxford Street",
    "address_line2": "Apt 4B",
    "city": "London",
    "state": "Greater London",
    "postal_code": "W1D 1BS",
    "country": "United Kingdom",
    "created_at": "2026-11-23T10:05:11+00:00"
  }
}
```

---

#### `POST /api/v1/profile`
Update customer profile, address, and upload/remove avatar.
- **Auth**: `Bearer <token>`
- **HTTP Method**: **Use `POST` for multipart uploads**. In PHP/Symfony, raw HTTP `PATCH` requests do not populate `$_FILES` or multipart form data, resulting in validation failure (HTTP 422). If desired, clients may use `POST /api/v1/profile` with form field `_method=PATCH`.
- **Content-Type**: `multipart/form-data` (or `application/json` when not uploading files)
- **Exact Form Field Names**:
  - `name`: `required|string|max:255`
  - `email`: `required|string|lowercase|email|max:255|unique:users,email,<user_id>`
  - `avatar`: `nullable|file|image|mimes:jpeg,png,jpg,webp|max:2048` (2 MB max binary image)
  - `remove_avatar`: `nullable|boolean` (Send `true` or `1` to delete existing avatar)
  - `phone`: `nullable|string|max:50`
  - `bio`: `nullable|string|max:1000`
  - `address_line1`: `nullable|string|max:255`
  - `address_line2`: `nullable|string|max:255`
  - `city`: `nullable|string|max:100`
  - `state`: `nullable|string|max:100`
  - `postal_code`: `nullable|string|max:30`
  - `country`: `nullable|string|max:100`
- **Email Modification & Verification Reset**:
  If the `email` field is modified, the backend automatically **resets `email_verified_at` to `null`** (unless `AUTO_VERIFY_EMAILS=true` is set on staging), sends a new verification link, and the customer is locked out of verified endpoints (`checkout`, `orders`, `profile/password`) until verified.
- **Success Response (200 OK)**:
```json
{
  "message": "Profile updated successfully.",
  "data": {
    "id": 1,
    "name": "Jane Elizabeth Reader",
    "email": "jane@example.com",
    "email_verified": true,
    "email_verified_at": "2026-11-23T10:05:11+00:00",
    "is_admin": false,
    "avatar_url": "https://.../storage/avatars/abc123xyz.jpg",
    "phone": "+44 20 7946 0912",
    "bio": "Senior bibliophile, collector of first editions.",
    "address_line1": "124 Oxford Street",
    "address_line2": "Suite 4B",
    "city": "London",
    "state": "Greater London",
    "postal_code": "W1D 1BS",
    "country": "United Kingdom",
    "created_at": "2026-11-23T10:05:11+00:00"
  }
}
```

---

#### `PUT /api/v1/profile/password`
Update customer password.
- **Auth**: `Bearer <token>` + Verified Email
- **Request Body**:
```json
{
  "current_password": "Secret123!",
  "password": "NewStrongPass99!",
  "password_confirmation": "NewStrongPass99!"
}
```
- **Validation**:
  - `current_password`: `required|current_password`
  - `password`: `required|min:8|confirmed`
- **Success Response (200 OK)**:
```json
{
  "message": "Password updated successfully."
}
```
- **Error Response (422 Wrong Password)**:
```json
{
  "message": "The given data was invalid.",
  "errors": {
    "current_password": [
      "The password is incorrect."
    ]
  }
}
```

---

#### `DELETE /api/v1/profile`
Delete or anonymize account.
- **Auth**: `Bearer <token>`
- **Request Body**:
```json
{
  "password": "Secret123!"
}
```
- **Accounting Retention Policy**:
  - If the user **has existing orders**: Personal data is permanently anonymized (`name: "Former Customer #1"`, `email: "anonymized_1_xxxx@example.invalid"`), credentials scrambled, and all Sanctum tokens revoked. Financial and tax records remain intact.
  - If the user **has no orders**: User row and tokens are permanently deleted from the database.
- **Success Response (200 OK)**:
```json
{
  "message": "Account deleted successfully."
}
```

---

## 5. Mobile Checkout Flow with Stripe SDK (Illustrative Kotlin Sample)

> [!NOTE]
> This sample is **illustrative**. The Android implementation may use Jetpack Compose, Kotlin Coroutines, Kotlin Flow, or Retrofit `suspend` functions.

### Architecture Sequence Diagram
```
Android App (Kotlin)         Backend (/api/v1)              Stripe Servers
      |                              |                            |
      |-- 1. POST /checkout/intent ->|                            |
      |   (name, address, city, zip) | (reserve stock 15 min)     |
      |                              |-- create PaymentIntent --->|
      |                              |<-- return client_secret ---|
      |<-- 2. {client_secret, id} ---|                            |
      |                                                           |
      |-- 3. Present Stripe PaymentSheet (SDK) ------------------>|
      |<-- 4. PaymentSheetResult.Completed -----------------------|
      |                                                           |
      |-- 5. POST /checkout/confirm ->|                            |
      |   (order_id)                 |-- verify PaymentIntent --->|
      |                              |   status == 'succeeded'    |
      |                              | (decrement stock lock)     |
      |                              | (release hold, clear cart) |
      |<-- 6. {data: OrderDetail} ---|                            |
```

### Illustrative Kotlin Sample
```kotlin
import android.content.Context
import androidx.activity.ComponentActivity
import com.stripe.android.PaymentConfiguration
import com.stripe.android.paymentsheet.PaymentSheet
import com.stripe.android.paymentsheet.PaymentSheetResult
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class CheckoutController(
    private val activity: ComponentActivity,
    private val apiService: BookStoreApiService
) {
    private var paymentSheet: PaymentSheet
    private var currentOrderId: Long? = null

    init {
        paymentSheet = PaymentSheet(activity) { result ->
            handlePaymentResult(result)
        }
    }

    /**
     * Call this when initializing the checkout flow.
     * Fetches public config and initializes Stripe SDK.
     */
    fun setupStripeFromBackendConfig(context: Context) {
        apiService.getConfig().enqueue(object : Callback<ConfigResponse> {
            override fun onResponse(call: Call<ConfigResponse>, response: Response<ConfigResponse>) {
                val stripeKey = response.body()?.data?.stripe_publishable_key
                if (!stripeKey.isNullOrBlank()) {
                    PaymentConfiguration.init(context, stripeKey)
                }
            }
            override fun onFailure(call: Call<ConfigResponse>, t: Throwable) {
                // Handle config failure
            }
        })
    }

    fun initiateCheckout(address: ShippingAddressDto) {
        apiService.createPaymentIntent(address).enqueue(object : Callback<PaymentIntentResponse> {
            override fun onResponse(call: Call<PaymentIntentResponse>, response: Response<PaymentIntentResponse>) {
                val data = response.body()?.data ?: return
                currentOrderId = data.order_id

                // Case 1: 100% Free Coupon Order
                if (data.free_order) {
                    navigateToOrderSuccess(data.order_id)
                    return
                }

                // Case 2: Standard Stripe PaymentSheet
                val config = PaymentSheet.Configuration(
                    merchantDisplayName = "BookStore",
                    allowsDelayedPaymentMethods = false
                )
                paymentSheet.presentWithPaymentIntent(data.client_secret!!, config)
            }

            override fun onFailure(call: Call<PaymentIntentResponse>, t: Throwable) {
                showError("Could not initiate checkout: ${t.message}")
            }
        })
    }

    private fun handlePaymentResult(result: PaymentSheetResult) {
        when (result) {
            is PaymentSheetResult.Completed -> {
                val orderId = currentOrderId ?: return
                // Send order_id (payment_intent_id is optional as backend holds it)
                val confirmReq = ConfirmOrderRequest(order_id = orderId)
                
                apiService.confirmOrder(confirmReq).enqueue(object : Callback<OrderDetailResponse> {
                    override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                        if (response.isSuccessful && response.body() != null) {
                            navigateToOrderSuccess(orderId)
                        } else {
                            // If confirmation failed, verify order status from server before declaring error
                            verifyOrderStatus(orderId)
                        }
                    }

                    override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                        // Network dropped: verify actual status from GET /orders/{id}
                        verifyOrderStatus(orderId)
                    }
                })
            }
            is PaymentSheetResult.Canceled -> {
                // User backed out of PaymentSheet.
                // Note: Stock hold will expire after 15 minutes.
                showToast("Payment cancelled.")
            }
            is PaymentSheetResult.Failed -> {
                showToast("Payment failed: ${result.error.localizedMessage}")
            }
        }
    }

    private fun verifyOrderStatus(orderId: Long) {
        apiService.getOrder(orderId).enqueue(object : Callback<OrderDetailResponse> {
            override fun onResponse(call: Call<OrderDetailResponse>, response: Response<OrderDetailResponse>) {
                val order = response.body()?.data
                if (order?.payment_status == "paid") {
                    navigateToOrderSuccess(orderId)
                } else {
                    showError("Payment verification incomplete. Please check your order history.")
                }
            }
            override fun onFailure(call: Call<OrderDetailResponse>, t: Throwable) {
                showError("Network error. Please check My Orders to verify order status.")
            }
        })
    }
}
```

---

## 6. Stock Reservations & Concurrency Details

### Real Behavior on Railway (No Background Scheduler)
1. **At Read Time (`scopeActive`)**: Available inventory is computed dynamically as:
   $$\text{availableStock} = \max(0, \text{catalogStock} - \sum \text{quantity of active holds by other users})$$
   A reservation is active **only if `expires_at > now()`**. Any reservation older than 15 minutes is **immediately ignored at query time** by SQL.
2. **No Background Worker / Daemon**:
   The Railway deployment does not run a continuous cron daemon. Expired rows in the `stock_reservations` table are physically pruned opportunistically whenever any user calls `POST /checkout/create-payment-intent` (`StockReservationService::pruneExpired()`).
3. **Abandoned Orders**:
   If a user starts checkout but never completes payment:
   - The pending `Order` record remains in the `orders` table with `status: pending`.
   - The associated `StockReservation` row expires after 15 minutes.
   - Once expired, those books become immediately available to other shoppers without needing a background job.
   - Abandoned orders do **not** permanently lock inventory.

---

## 7. Business Rules & Domain Logic

### 1. Stock Limits & Availability
- Each book has a real-time `stock` counter.
- Cart additions are **cumulative**: adding 2 copies of an item that already has 3 copies in your cart requests 5 copies. If `5 > stock`, the API rejects with HTTP 422:
  `Not enough stock available for "Book". Only X left (Y already in your bag).`
- When browsing the catalog, `in_stock == true` indicates `stock > 0`.

### 2. Coupon Discount Rules
- **Fixed discount** (`type: "fixed"`): Subtracts a fixed dollar amount, capped at the cart total (total cannot go below 0).
- **Percentage discount** (`type: "percent"`): Calculates `round((value / 100) * subtotal, 2)`, capped at cart total.
- **Minimum cart threshold**: If a coupon specifies `min_amount`, subtotal must be $\ge \text{min\_amount}$; otherwise rejected with:
  `Cart total must be at least $XX.XX to use this coupon.`
- **Single active coupon**: A cart holds at most one coupon at any time. Applying a new coupon overwrites the previous one.

### 3. Order Status & Progression Timeline
The `Order` model tracks three distinct status variables:

| Status Key | Possible Values | Meaning |
|---|---|---|
| `status` | `pending`, `paid`, `processing`, `shipped`, `delivered`, `cancelled` | Primary lifecycle state |
| `payment_status` | `pending`, `paid`, `failed`, `refunded` | Financial state |
| `fulfillment_status` | `unfulfilled`, `processing`, `shipped`, `delivered`, `cancelled` | Warehouse & courier state |

#### Timeline Step Index (`step_index`)
The Android app can directly bind this integer to a timeline progress indicator:
- `1` = **Order Confirmed** (`status: pending | paid`)
- `2` = **Processing & QC** (`status: processing`)
- `3` = **Shipped / In Transit** (`status: shipped`)
- `4` = **Delivered** (`status: delivered`)
- `-1` = **Cancelled** (`status: cancelled`)

#### Customer Cancellation Policy
> **NOT FOUND**: Customer-initiated self-service order cancellation is intentionally not exposed in the API. Orders are committed upon payment. Cancellations and refunds are handled exclusively by administrators through the back-office dashboard.

### 4. Review Restrictions
- One review per user per book (`updateOrCreate`). Submitting again modifies the user's existing review.
- `is_verified_purchase` is evaluated strictly by the server. Clients cannot spoof this flag.
- Helpful votes are stored per customer ID in the database; duplicate attempts return HTTP 422.

---

## 8. Client Implementation Notes

### Data Types & Formatting
- **Money & Prices**: All monetary amounts (`price`, `subtotal`, `discount`, `discount_amount`, `total`, `total_amount`) are returned as JSON floating-point numbers with 2 decimal places (e.g. `14.99`, `51.72`, `0.0`). In Kotlin, parse and store these with `BigDecimal` or `Double` (never integer cents).
- **Dates & Timestamps**: Timestamps follow ISO 8601 UTC (e.g. `"2026-11-23T10:05:11.000000Z"`). Estimated delivery dates use `"YYYY-MM-DD"`.
- **Nullable Fields**: Android models must mark the following fields as nullable (`?`):
  - `carrier`, `tracking_number`, `tracking_url`, `estimated_delivery_at`, `shipped_at`, `delivered_at`
  - `avatar_url`, `phone`, `bio`, `address_line1`, `address_line2`, `city`, `state`, `postal_code`, `country`
  - `coupon_code`
  - `sample_excerpt`
- **Pagination**: Use `meta.current_page`, `meta.last_page`, and `links.next` for Android Paging 3 / RecyclerView infinite scroll.

### Client Error Detection Mapping
Map HTTP response codes cleanly to UI states:
| HTTP Status | Trigger Condition | Client Action |
|---|---|---|
| **401 Unauthorized** | Token expired (after 30 days) or missing | Clear local preferences/DataStore, navigate user to `LoginActivity` |
| **403 Forbidden (`code: email_not_verified`)** | User email is not verified | Show email verification dialog with button to invoke `POST /auth/email/verification-notification` |
| **403 Forbidden** | General access denied (e.g. invoice not owned) | Show toast/dialog: "Access denied." |
| **404 Not Found** | Book, Order, or Cart Item does not exist | Show empty state or pop fragment |
| **400 Bad Request** | Guest endpoint called while logged in | Continue to home dashboard |
| **422 Unprocessable** | Input validation or business rule rejection | Parse `errors` dictionary and bind error strings under respective text inputs |
| **429 Too Many Requests**| Rate limit exceeded | Back off and retry after reading `Retry-After` header |
| **503 Unavailable** | Payment gateway not configured | Show dialog: "Payment gateway is undergoing maintenance. Please try again later." |

---

## 9. Data Models & Resource Schemas

### `UserResource`
| Field | Type | Nullable | Example | Description |
|---|---|---|---|---|
| `id` | Integer | No | `1` | Unique User ID |
| `name` | String | No | `"Jane Reader"` | Full name |
| `email` | String | No | `"jane@example.com"` | Normalized lowercase email |
| `email_verified` | Boolean | No | `true` | Quick boolean flag |
| `email_verified_at` | String (ISO-8601) | Yes | `"2026-11-23T10:05:11+00:00"` | Timestamp of verification |
| `is_admin` | Boolean | No | `false` | Administrative status |
| `avatar_url` | String (URL) | Yes | `"https://.../storage/avatars/1.jpg"` | Absolute URL (forced HTTPS in prod) |
| `phone` | String | Yes | `"+1-555-0199"` | Contact telephone number |
| `bio` | String | Yes | `"Avid reader"` | Customer bio |
| `address_line1` | String | Yes | `"124 Oxford Street"` | Street address |
| `address_line2` | String | Yes | `"Apt 4B"` | Apartment / Suite |
| `city` | String | Yes | `"London"` | City |
| `state` | String | Yes | `"Greater London"` | State / Province |
| `postal_code` | String | Yes | `"W1D 1BS"` | ZIP / Postal code |
| `country` | String | Yes | `"United Kingdom"` | Country name |
| `created_at` | String (ISO-8601) | No | `"2026-11-23T10:05:11+00:00"` | Registration timestamp |

---

### `BookResource` (Catalog & Wishlist Item)
| Field | Type | Nullable | Example | Description |
|---|---|---|---|---|
| `id` | Integer | No | `1` | Book ID |
| `title` | String | No | `"The Great Gatsby"` | Book Title |
| `slug` | String | No | `"the-great-gatsby-demo"` | Unique URL slug |
| `author` | String | No | `"F. Scott Fitzgerald"` | Author name |
| `isbn` | String | Yes | `"9780743273565"` | Standard ISBN-10 or ISBN-13 |
| `price` | Float / Double | No | `14.99` | Retail price in USD |
| `stock` | Integer | No | `25` | Available inventory count |
| `in_stock` | Boolean | No | `true` | `stock > 0` |
| `cover_url` | String (URL) | No | `"https://.../covers/gatsby.jpg"` | Cover image link or default placeholder |
| `has_sample` | Boolean | No | `true` | Whether reader excerpt exists |
| `category` | Object | Yes | `{"id": 1, "name": "Fiction", "slug": "fiction"}` | Associated Category |
| `average_rating` | Float / Integer | No | `5.0` | Average review rating (0.0 to 5.0) |
| `reviews_count` | Integer | No | `1` | Number of verified/reader reviews |
| `created_at` | String (ISO-8601) | No | `"2026-11-23T10:05:10+00:00"` | Date added |

---

### `CartResource`
| Field | Type | Nullable | Example | Description |
|---|---|---|---|---|
| `items` | Array of Objects | No | `[...]` | List of items in bag |
| `items[].book_id` | Integer | No | `1` | Book ID |
| `items[].title` | String | No | `"The Great Gatsby"` | Title snapshot |
| `items[].slug` | String | No | `"the-great-gatsby-demo"` | Slug for navigation |
| `items[].price` | Float / Double | No | `14.99` | Unit price |
| `items[].quantity` | Integer | No | `2` | Selected quantity |
| `items[].subtotal` | Float / Double | No | `29.98` | `price * quantity` |
| `items[].cover_url` | String (URL) | No | `"https://..."` | Cover image URL |
| `items[].stock` | Integer | No | `25` | Current catalog inventory |
| `items[].in_stock` | Boolean | No | `true` | Whether inventory satisfies requested quantity |
| `subtotal` | Float / Double | No | `29.98` | Gross item subtotal before discounts |
| `discount` | Float / Double | No | `0.00` | Discount amount applied by coupon |
| `coupon_code` | String | Yes | `"WELCOME10"` | Code of applied coupon |
| `total` | Float / Double | No | `29.98` | `max(0, subtotal - discount)` |
| `items_count` | Integer | No | `2` | Cumulative sum of item quantities |

---

### `OrderDetailResource`
| Field | Type | Nullable | Example | Description |
|---|---|---|---|---|
| `id` | Integer | No | `1` | Order ID |
| `status` | String | No | `"shipped"` | Lifecycle status |
| `payment_status` | String | No | `"paid"` | Payment state |
| `fulfillment_status`| String | No | `"shipped"` | Fulfillment stage |
| `total_amount` | Float / Double | No | `27.49` | Charged total amount |
| `discount_amount` | Float / Double | No | `0.00` | Applied discount amount |
| `coupon_code` | String | Yes | `null` | Applied coupon |
| `customer_name` | String | No | `"Jane Reader"` | Snapshot of buyer name |
| `customer_email` | String | No | `"jane@example.com"` | Snapshot of buyer email |
| `shipping_address` | String | No | `"Jane Reader, 124 Oxford St..."` | Human-readable address |
| `carrier` | String | Yes | `"DHL Express"` | Shipping carrier name |
| `tracking_number` | String | Yes | `"DHL-89234710"` | Tracking code |
| `tracking_url` | String (URL) | Yes | `"https://www.dhl.com/..."` | Direct link to carrier parcel tracker |
| `estimated_delivery_at` | String (YYYY-MM-DD) | Yes | `"2026-11-25"` | Expected delivery date |
| `shipped_at` | String (ISO-8601) | Yes | `"2026-11-22T10:05:11.000000Z"` | Dispatch timestamp |
| `delivered_at` | String (ISO-8601) | Yes | `null` | Final delivery timestamp |
| `step_index` | Integer | No | `3` | Progression timeline index (1 to 4, -1) |
| `items` | Array of Objects | No | `[...]` | Historical line items |
| `items[].book_id` | Integer | No | `1` | Original Book ID |
| `items[].title` | String | No | `"The Great Gatsby"` | Immutable title at purchase time |
| `items[].author` | String | No | `"F. Scott Fitzgerald"` | Immutable author at purchase time |
| `items[].quantity` | Integer | No | `1` | Purchased quantity |
| `items[].price` | Float / Double | No | `14.99` | Locked unit price |
| `items[].book.slug`| String | No | `"the-great-gatsby-demo"` | Slug for navigation |
| `items[].book.cover_url` | String (URL) | No | `"https://..."` | Cover image URL |
| `invoice_url` | String (URL) | No | `"https://.../orders/1/invoice"` | PDF invoice download link |
| `created_at` | String (ISO-8601) | No | `"2026-11-23T10:05:11.000000Z"` | Order placement date |

---

## 10. Localization & RTL (Accept-Language)

The backend inspects the standard HTTP header:
```http
Accept-Language: fr
```
- **Supported Locales**:
  - `en` — English (Default)
  - `fr` — French (Français)
  - `ar` — Arabic (العربية, RTL UI layout required on Android)
- **Translated Elements**:
  - Validation error messages (e.g. `Le champ email est obligatoire`, `يرجى تقديم بريد إلكتروني صالح`).
  - Action confirmation messages (`Book added to cart successfully.`, `Shopping bag cleared.`).
  - System exception messages.
- **Book Catalog Content**:
  - Book titles, descriptions, and author names are currently stored in English in the database catalog.

---

## 11. Staging & Testing Guide for Android Engineers

### 1. Pre-Seeded Accounts Note
- Pre-seeded accounts (`reader@example.com`, `admin@example.com`) only exist on environments where `ALLOW_DEMO_SEED=true` was enabled during database migration/seeding.
- **Passwords are NOT documented here**. Developers and QA testers should simply **register a fresh account** using `POST /api/v1/auth/register` on staging. When `AUTO_VERIFY_EMAILS=true` is enabled on the staging server, registered users are instantly email-verified.

### 2. Stripe Test Card Numbers
When configuring the Android Stripe SDK with `stripe_publishable_key`:
- **Standard Successful Charge**: `4242  4242  4242  4242` (Any future expiration date `12/30`, any 3-digit CVC `123`).
- **3D Secure / SCA Authentication Challenge**: `4000  0027  6000  3184` (Triggers Stripe biometric/SMS challenge modal).
- **Declined Card (Insufficient Funds)**: `4000  0000  0000  0002`.

---

## 12. Quick Reference Table & Known Limitations

### Complete Endpoints Quick Reference

| Method | Full Endpoint | Auth | Verified Req.? | Rate Limit | Purpose |
|---|---|---|---|---|---|
| `GET` | `/api/v1/config` | Public | No | 120/min | App settings & Stripe publishable key |
| `GET` | `/api/v1/categories` | Public | No | 120/min | Active category listing |
| `GET` | `/api/v1/books` | Public | No | 120/min | Filterable & sortable catalog |
| `GET` | `/api/v1/books/{book}` | Public | No | 120/min | Book details, reviews, related |
| `GET` | `/api/v1/books/{book}/reviews` | Public | No | 120/min | Reviews for a book |
| `POST` | `/api/v1/auth/register` | Guest | No | 10/min | Create account & get token |
| `POST` | `/api/v1/auth/login` | Guest | No | 10/min | Customer login & get token |
| `POST` | `/api/v1/auth/forgot-password` | Guest | No | 10/min | Request password reset email |
| `POST` | `/api/v1/auth/reset-password` | Guest | No | 10/min | Reset password with token |
| `POST` | `/api/v1/auth/logout` | Bearer | No | 60/min | Revoke Sanctum access token |
| `GET` | `/api/v1/auth/user` | Bearer | No | 60/min | Fetch logged-in user profile |
| `POST` | `/api/v1/auth/email/verification-notification` | Bearer | No | 6/min | Resend verification email |
| `GET` | `/api/v1/cart` | Bearer | No | 60/min | Retrieve server-side DB cart |
| `POST` | `/api/v1/cart/items` | Bearer | No | 60/min | Add item to cart |
| `PATCH` | `/api/v1/cart/items/{book}` | Bearer | No | 60/min | Update item quantity |
| `DELETE` | `/api/v1/cart/items/{book}` | Bearer | No | 60/min | Remove item from cart |
| `POST` | `/api/v1/cart/clear` | Bearer | No | 60/min | Empty cart |
| `POST` | `/api/v1/cart/coupon` | Bearer | No | 15/min | Apply fixed or % coupon |
| `DELETE` | `/api/v1/cart/coupon` | Bearer | No | 15/min | Remove coupon |
| `POST` | `/api/v1/checkout/create-payment-intent` | Bearer | **Yes** | 15/min | Reserve stock & start Stripe intent |
| `POST` | `/api/v1/checkout/confirm` | Bearer | **Yes** | 15/min | Confirm order & claim inventory |
| `GET` | `/api/v1/orders` | Bearer | **Yes** | 60/min | Customer orders list |
| `GET` | `/api/v1/orders/{id}` | Bearer | **Yes** | 60/min | Customer order detail |
| `GET` | `/api/v1/orders/{id}/invoice` | Bearer | **Yes** | 60/min | Download PDF tax invoice |
| `GET` | `/api/v1/wishlist` | Bearer | No | 60/min | Get wishlisted books |
| `POST` | `/api/v1/wishlist/toggle` | Bearer | No | 60/min | Toggle book in wishlist |
| `DELETE` | `/api/v1/wishlist/{book}` | Bearer | No | 60/min | Remove from wishlist |
| `POST` | `/api/v1/wishlist/{book}/move-to-cart` | Bearer | No | 60/min | Transfer from wishlist to bag |
| `POST` | `/api/v1/books/{book}/reviews` | Bearer | No | 10/min | Add or update review |
| `POST` | `/api/v1/reviews/{id}/helpful` | Bearer | No | 10/min | Upvote helpful review |
| `GET` | `/api/v1/profile` | Bearer | No | 60/min | Get profile info |
| `POST` | `/api/v1/profile` | Bearer | No | 60/min | Update profile & avatar (multipart) |
| `PUT` | `/api/v1/profile/password` | Bearer | **Yes** | 60/min | Change password |
| `DELETE` | `/api/v1/profile` | Bearer | No | 60/min | Delete / anonymize account |

---

### Known Limitations / Features Not Available for Mobile

The following features were purposefully **NOT** included in the v1 mobile REST API and must be treated as **NOT FOUND**:

1. **Admin Endpoints**: No mobile endpoints exist for administrative CRUD (`/admin/books`, `/admin/orders`, `/admin/categories`). The mobile app is designed strictly for consumers.
2. **Push Notifications (FCM)**: Firebase Cloud Messaging tokens or device push registration endpoints are NOT FOUND. Order status updates are tracked via polling or email.
3. **In-App Customer Order Cancellation**: Self-service order cancellation is NOT FOUND. Cancellations require contacting customer support.
4. **Social Login (OAuth / Google Sign-In)**: Google, Apple, or Facebook OAuth endpoints are NOT FOUND. Authentication is strictly email and password.
5. **Multi-Currency**: Multi-currency conversion is NOT FOUND. All transactions are priced and settled in `USD`.
