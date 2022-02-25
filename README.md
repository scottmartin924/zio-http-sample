# zio-http-sample
This is a sample web app meant to experiment with the following technologies:

- [zio-http](https://github.com/dream11/zio-http)
- [doobie](https://tpolecat.github.io/doobie/)
- [circe](https://circe.github.io/circe/)

> :warning: **Nobody should think anything here is "correct".**

## Getting Started
Before starting the web app you'll need a local postgres instance running and then execute [db_setup.sql](src/main/resources/db_setup.sql).
This will create a database called _sample_ and populate it with some tables and data. To actually start the web
server execute `sbt run`; this will start a webserver at `localhost:8080`. 

The app is a very rudimentary todo manager that just exposes CRUD operations
on some `Todo(id: Long, entry: String)` objects. In order to use any of the endpoints you'll need a valid
JWT on the `X-Access-Token` header. To get a JWT use the `POST /login` endpoint with body
```json
{
  "username": "scott",
  "password": "test"
}
```
or any of the other username/password pairs from `db_setup.sql`

### Endpoints
The exposed endpoints with sample requests and required roles (see [Roles](#roles) for more info) are given below:

- `POST /login`
```json
{
    "username": "scott",
    "password": "test"
}
```
- `GET /todo` (requires `admin` or `supervisor` role)
- `GET /todo/{id: Long}`
- `DELETE /todo/{id: Long}` (requires `admin` role)
- `POST /todo` (requires `admin` or `supervisor` role)
```json
{
    "entry": "Finish this README"
}
```
- `PATCH /todo/{id: Long}` (requires `admin` role)
```json
{
    "entry": "a new update"
}
```

### Roles
An _extremely_ primitive role system is setup just to experiment with. In particular, note that the permission checks
on endpoints uses roles instead of more granular permissions...again this was just to play around with. Regardless, to
modify a user's roles just update the `user_role` table. Note that `user_role.role` isn't a FK to anything so make
it whatever you'd like and that string will get added to the `roles` array in the JWT when you login.

To modify the roles required for a particular route find the route in [Main.scala](src/main/scala/Main.scala) and
modify the `roles` zio that's created before running the `TodoController` function. For example,
```scala
case Method.GET -> !! / "todo" => roles("admin" or "supervisor") *> TodoController.getAll
```
means that `GET /todo` requires an `admin` or `supervisor` role. Feel free to edit, remove, modify as you see fit.
