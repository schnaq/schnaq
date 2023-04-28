<p align="center">
  <a href="https://app.schnaq.com">
    <picture>
      <source media="(prefers-color-scheme: dark)" srcset="https://s3.schnaq.com/schnaq-common/logos/schnaq_white.webp">
      <img src="https://s3.schnaq.com/schnaq-common/logos/schnaq.webp" height="80">
    </picture>
    <h1 align="center">schnaq</h1>
    <div align="center">Education as interactive, as it's supposed to be!</div>
  </a>
</p>

<img width="1348" alt="product" src="https://user-images.githubusercontent.com/1507474/213150339-c281ef4d-3759-4085-bd99-935f025eb2e6.png">

This is the official repository of the schnaq project. We are constantly working on improving the platform and adding new features. Please feel free to contribute to the project by opening issues or pull requests.

Find the project at https://app.schnaq.com

We are working on a minimal version of schnaq without the dependencies to external systems (e.g. Stripe or CleverReach).

## Development

The dev-Setup has different parts for Backend and Frontend. Please make sure to
include the `dev` alias when starting a REPL, e.g. `clj -M:dev:run-server`.

### Database

We are using `datomic pro` as our database, which can be used during development and testing.
Get a fresh license for datomic pro starter via this URL: https://my.datomic.com/

Then, after registering visit: https://my.datomic.com/account.
Follow the instructions to set your `~/.m2/settings.xml` server to contain credentials for my.datomic.com.
You will need to create a new account after one year.

#### Development: Starting the local transactor (for the database)

Go to https://my.datomic.com/account and copy the `wget` command with the desired version.
Execute it in the schnaq folder and name it datomic-pro, or wherever you desire.

- unzip
- Enter your license-key into the datomic-pro/dev-transactor.properties (Copy it over from datomic-pro/config/samples/dev-transactor-template.properties)

Start the transactor to dev with: `bin/transactor dev-transactor.properties`.

Please note that Java 11 is required to run the transactor.
Either use a SDK manager or export Java 11 manually: `export JAVA_HOME=$(/PATH/TO/java_home -v11)`.

#### Production

To connect to a database, provide the proper connection string.
You can find the connection string when starting your datomic instance.

We put the connection string into the project's environment variables and build them into the Docker image.
The connection string from us is a **format string**.
We replaced `<DB-NAME>` with `%s`.
Set your database in the config namespace.

### Backend

Start the backend-server with one of these two options:

_With REPL_

Start the run configuration "CLJ REPL" and execute the `-main` method in
`schnaq.api`.
To do this manually, you can put the following commands into the REPL in IDEA:

```clojure
(require '[schnaq.api])
(schnaq.api/-main)
```

_Without REPL_

`clj -M:run-server` on the terminal

### Frontend

The Frontend works with shadow-cljs for hot code reload.

1. Run `yarn install` to get javascript dependencies.
2. Run `clj -M:frontend:dev` to compile the cljs and start the watcher.
3. Shadow-cljs starts a nrepl-server.
   You can connect to localhost and the port output to the `.shadow-cljs/nrepl.port` file.
4. In the opened _CLJ_ REPL you can execute `(shadow/repl :app)` to switch to the hot development REPL for _CLJS_.

#### Stylesheets

To automatically create the stylesheets, enable a file-watcher for the `resources/public/css` directory.

To do this and watch for changes, use this command:

    yarn css:watch

To make a minified build:

    yarn css:minify

### Linting Styles locally

If you want to lint the style locally, you need to run `yarn install --dev` to install stylelint.

Then just execute `yarn stylelint "public/css/*.scss"` in the project root.

### Testing

#### Backend

Run `clj -M:test`

Run a single test or test namespace `clj -M:test --focus [namespace]/[function-name]`

E.g.: `clj -M:test --focus schnaq.api-test/update-meeting-test`

#### Frontend

Run:

```bash
yarn shadow-cljs compile test
node target/test/compiled/test.js
```

### CSS Optimization

Purgecss will be installed as a dev dependency.
You can run the following command from the schnaq route to find unused css in the app.
(Build the app once before)

    yarn css:purge

This outputs all unused css classes.

## Known Problems

### Google Closure: Advanced Compilation

In the process of optimizations, the function names are reduced to a couple of
unpredictable characters. Sometimes, when doing JavaScript-Interop, these
functions are no longer accessible (e.g. when calling `(.getWritable this)`).
This can also happen to fields / constants, which are renamed during the
compilation.

To avoid errors, use the [oops](https://github.com/binaryage/cljs-oops) library,
especially `oget`, `ocall`, and `oset` (there are more useful functions).

### Debugging a production build

We use the google closure compiler collection to build minimized and optimized code.
Sometimes, in the minified version, the resulting code contains problems, e.g. unresolvable functions.
To debug this efficiently, use the following command to prepare a production build with debug information:

    yarn shadow-cljs release app --debug

Serve the assets then via an nginx. You can pick the nginx-configuration from
the root of this repository, e.g. with this call:

    docker run -it --rm -v $(pwd):/usr/share/nginx/html -v $(pwd)/nginx/schnaq.conf:/etc/nginx/conf.d/default.conf -p 8888:80 nginx

### License

This code and all management code belonging to the schnaq repository is published under the AGPL 3.0 (GNU AFFERO GENERAL PUBLIC LICENSE Version 3)
