# TODO: Must terminate this guide

Contributing Code to OWGE project
=============================

# Before you start
Thank you for contributing source code to OWGE project.

Please notice that this project was not initially intended to become Opensource, so developing is a bit tricky, development in Linux machine has not been tested

&nbsp;<details>
&nbsp;<summary>__Required tools__</summary>

## Developing

1. Docker
2. git

## Backend

1. STS 3.9.4 or greater
2. [Maven template variables](https://marketplace.eclipse.org/content/mmm-templatevariables)
3. [Sonarlint plugin for Eclipse](https://marketplace.eclipse.org/content/sonarlint)
4. XAMPP or LAMP (MySQL version must be 5.7)
5. Tomcat 7 (Tomcat 8 has not been tested)
6. Drupal 8 (TODO I have to better elaborate this one)

## Frontend

1. Angular CLI 5. You can install latest Angular CLI, as the client itself will use downgraded version
2. Document This extension
3. Tslint extension
4. Angular Language Service

&nbsp;</details>

&nbsp;<details>
&nbsp;<summary>__Steps to get a working development environment__</summary>

1. Download Kevinsuite Java and install it to your local maven repository, alternatively you can import the projects in your Eclipse. This step is required because Kevinsuite is not in maven central repository (for now)
2. In your computer add the IP address 10.74.32.252/24 to the list of IP Addresses it has (This is a temporary requirement, as must Fix the Dockerfile of development container)
3. Open a bash a cd into __docker-ci/dev/main_reverse_proxy__ , run `$ build.sh` and then launch the container with any of the run commands (depending on your platform)
4. Extract the tomcat and add it to STS
5. Create a database for the game, with `utf8_general_ci`, and import the file __business/database/sgalactica_java.sql__ it everything is correct, you should now import the required minimal data, to do so import __business/database/sql/insert-data.sql__
6. Add the following datasources to your tomcat
```xml
<Resource name="jdbc/owgejava" auth="Container" type="javax.sql.DataSource"
        maxActive="10" maxIdle="3" maxWait="10" username="root"
        validationQuery="SELECT 1" testOnBorrow="true" testOnConnect="true"
        testWhileIdle="true" logValidationErrors="true" password="1234"
        driverClassName="com.mysql.jdbc.Driver" url="jdbc:mysql://127.0.0.1:3306/your_db_name" />
```
7. Now you are ready to launch the backend, add the game-rest to the Tomcat, and launch it
8. To launch the frontend, it's recommended to define an alias in your bash profile for example:
```bash
alias owgengserve='cd /path/to/game-frontend; npm run start -- --host 10.74.32.252 --public-host 192.168.99.100 --live-reload false';
```
public host must point to your docker machine IP, that will be the url you are going to use in the browser, for example http://192.168.99.100
9. Drupal part (required for login, MUST CONTINUE THIS GUIDE HERE!, as I have to upload the required modules to Github too)
&nbsp;</details>

&nbsp;<details>
&nbsp;<summary>__Coding rules__</summary>
* __NOTICE:__ Before you start reading the rules, don't trust in the current code in the `game-frontend folder`, as it has old code, and not all code in that folder is compliant with this rules, migration is work in progress when old code is required to be modified.
* :warning: :warning:  It's absolutely __mandatory__ to fully respect **tslint** and **sonarlint** rules, not doing so will mean to completely ignore the pull request till the SonarLint and tslint issues has been fixed by the contributor :warning: :warning:.
* Code and code documentation __MUST__ be in **English*
* Files&Directories names __MUST__ be lowercase dashed. Examples: game-frontend, some-long-name-dir, excluding .java files
* All __public__ classes and methods must have the Javadoc and the TSDoc completely defined, @since tag is mandatory
* In Angular don't use the global styles file, unless you are apllying style to the whole interface, and not to a single component
* When creating Angular files it's important to follow the following rules
* Modules should be places in __game-frontend/src/app/modules/`module_name`__
* Components, Services, Stores, pipes, Types, Interfaces, etc must be placed __always__ inside modules folder
* In Angular you must follow the way Angular defines the filenames, Example: some.component.ts, user.service.ts, but non Angular types must also follow this convention:
    * Types must be defined like `interface Some` and filename will be `some.type.ts`
    * Interfaces must be defined like `interface SocketHandler` and file will be `socket-handler.interface.ts`
    * Pojos must be defined like `class User` and file will look like `user.pojo.ts`
    * All other type of objects, will have in the class name the suffix of its type, and the filename will be `class-name.type-of-class.ts`, for example `class DateUtil` date.util.ts
    * Util class must be abstract and have a private constructor
* HTML attributes (including class and id) must be lowercase dashed, for example: 

```html  
<div class="some-class-name" custom-attribute="custom-value"><div>
```

* In Angular all private and protected properties and methods __MUST HAVE__ an initial underscore for example:

```typescript
class Some {
    protected _bar;
    private _foo;
    
    private _baz(): void {

    }
}
```
* When defining properties and methods, must be ordered by its visibility modifier, so a fully defined class would loo like this
```typescript
class SuperService {
    public static someVar: number;
    protected static _otherVar: string;
    private static readonly _SOME_STATIC_CONST = 12031993;
    
    public static staticPubMethod(): foo {...}
    protected static _staticProtMethod(): User {...}
    private static _saySecret(input: string): string {...}

    public instancePub = true;
    protected _instanceProt: Defined;
    private _instancePriv: string;

    // Please >> Note << the constructor MUST be the first non-static method
    public constructor(...){}

    public sayHi(): string {...}
    protected _sayFoo(): number {...}
    private _otherFoo(): void {...}
}
```
* When a class implements an interface this must have the interface name as a Suffix Example `class SomeSocketMessage implements SocketMessage `

* When the method is not a `getter` nor a `setter` it is forbidden to start its name with get or with set, use other words, like find, or define
```java
// Java
class SomeJavaRestService {
    private Integer number = 8;
    private Connection connection;

    public Integer getNumber() {
        return number;
    }

    // NOT  a real setter as it has logic
    public void setNumber(Integer param) {
        dbCon.query("DELETE FROM some_table WHERE number " + param);
        dbCon.query("UPDATE some_other_table SET number = number + " + param);
        number = param;
    }
    
    // Correct method name
    public void defineNumber(Integer param) {
        dbCon.query("DELETE FROM some_table WHERE number " + param);
        dbCon.query("UPDATE some_other_table SET number = number + " + param);
        number = param;
    }
}
```
&nbsp;</details>