# Poodle

## Setup

### Prerequisites
The following software has to be installed to set up Poodle:

- [Git](https://git-scm.herokuapp.com/downloads)
- [PostgreSQL](http://www.postgresql.org/download/)  >= 9.4
- [Java Development Kit](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) >= 8

### Notes for Windows Users
- The setup scripts rely on Unix tools. Using the Git Bash which is part of the Windows Git installation should work fine.
Alternatively use CYGWIN or something similiar.
- On Windows, PostgreSQL doesn't add the path to its binaries (usually C:/Program Files/PostgreSQL/9.4./bin)
to the PATH variable by default, which means the scripts won't be able to find them. You have to add it to the PATH
variable yourself (Google is your friend).

### Clone the Repository

    git clone git@github.com:whs-poodle/poodle.git
    cd poodle/

### Set up the Database
The setup script will create a new database user *poodle* and a database *poodle* and then initialize the tables etc.

    ./setup_db.sh

The password you provide for the database user will also automatically be written to the main configuration file
*application.properties* (see [Configuration](#configuration)).

### Build Poodle
Now build Poodle itself using Gradle. Note that this could take a few minutes on the first run since all the dependencies have to be downloaded.

    ./gradlew build

### Start Poodle
You can now run Poodle for the first time:

    ./run.sh

The startup can take a few seconds. When it is finished, go to [http://localhost:8080](http://localhost:8080) to
see the login screen.

### Creating a user
Poodle currently heavily relies on LDAP authentication (see [Configuration](#ldap-authentication)). There is only very basic support for local accounts, meaning you have to create them yourself with the *poodlecli* tool. For example, to create a new instructor:

    ./poodlecli.sh createInstructor

See the help for all available commands:

    ./poodlecli.sh --help

## Configuration

All configurations must be set in the *application.properties* in the root directory (note that there is also a *src/main/resources/application.properties* which should **not** be edited). This is a simple Java properties file with key value pairs.

### Database

The setup script has already written your password to *application.properties*, so the database should just work. If you need more configuration, e.g. because you are using a remote database, you may use the following properties:
```
spring.datasource.url=jdbc:postgresql://host/<database>
spring.datasource.username=<username>
spring.datasource.password=<password>
  ```

Note that the database name and username should both be *poodle* since the setup script sets it up this way. If you want to use other names you will have to set up the database on your own.

For detailed information on which properties can be set, see the *spring.datasource.** section in the [Spring Boot Documentation](http://docs.spring.io/spring-boot/docs/current/reference/html/common-application-properties.html).

### General

#### Server Locale
The Server Locale currently only defines the language that Emails are sent in, but it may be used for other purposes later.

    poodle.serverLocale=de

#### Base URL
The URL that Poodle is running on. This is currently only used to generate the links in the Emails, but may be used for other puporses later.

    poodle.baseUrl=https://poodle.w-hs.de/

### LDAP Authentication

LDAP authentication can be enabled with the following properties:

#### URLs

You may specify multiple URLs as backup.

```
poodle.login.ldap.urls[0]=ldaps://<ldap-host>:50003
poodle.login.ldap.urls[1]=ldaps://<ldap-backup-host>:50003
```

#### Username and Password

```
poodle.login.ldap.userDn=<ldap-user>
poodle.login.ldap.password=<password>
```

#### Base DN and User Search Filter

This defines where and how users are searched for in the LDAP directory. In the user search filter use *{0}* as a placeholder for the username.

```
poodle.login.ldap.baseDn=dc=ldap,dc=poodle,dc=github
poodle.login.ldap.userSearchFilter=(&(objectClass=person)(cn={0}))
```

### Student and Instructor Groups

Poodle uses these groups to determine if a user is a student or an instructor. It will be matched against the LDAP *memberOf* attribute. If the user is in neither of these groups, Poodle will deny the login.

```
poodle.login.ldap.studentGroup=cn=Students,ou=Groups,dc=ldap,dc=poodle,dc=github
poodle.login.ldap.instructorGroup=cn=Instructors,ou=Groups,dc=ldap,dc=poodle,dc=github
```

### *Forgot Password* Link
If you have a separate website where users can manage their LDAP account, change their password etc. you may want to set a *Forgot Password* link with the following:

    poodle.forgotPasswordLink=http://forgot.password/

The link will automatically be displayed below the login form.

### Email

Email support currently requires [LDAP Authentication](#ldap-authentication) since the users' Email addresses are read via LDAP. You also have to set the [Server Locale](#server-locale) and the [Base URL](#base-url).

A basic Email configuration may look like this:
```
poodle.emailEnabled=true
poodle.emailNoReplyAddress=noreply@w-hs.de
spring.mail.host=<smtp-host>
spring.mail.port=<smtp-port>
spring.mail.username=<username>
spring.mail.password=<password>
```

The port is optional and must only be specified if it is not the SMTP default (25). The no-reply address must be set and will be used as the reply adress for automatically generated Emails.

You may also have to use *spring.mail.properties* for advanced configuration. For example, to enable StartTLS:

    spring.mail.properties[mail.smtp.starttls.enable]=true

See the table [here](https://javamail.java.net/nonav/docs/api/com/sun/mail/smtp/package-summary.html) on details which other properties can be set with *spring.mail.properties*.

## Development

If you want to make changes to Poodle, make sure you are activating the *dev* profile which enables some additional logging and other configuration (see [application-dev.properties](src/main/resources/application-dev.properties)).

If you are developing in an IDE like Eclipse the easiest way to enable the *dev* profile is to set the following in your *application.properties*:

	spring.profiles.active=dev

Alternatively you can run Poodle with the parameter *--spring.profiles.active=dev*, but this may require additional setup in your IDE.

### Command Line

If you want to test your changes from the command line, you can simply run the Gradle task *bootRun* which will compile Poodle and start it. The *dev* profile will be enabled automatically.

    ./gradlew bootRun

### Eclipse

If you want to develop with Eclipse, make sure you use *Eclipse IDE for Java EE Developers* because the standard version has no support for editing HTML files. You can generate the Eclipse project files by running:

    ./gradlew eclipse

Afterwards you can import Poodle as a simple Eclipse Project. Note that you have to recreate the project files everytime changes to the [build file](build.gradle) have been made.

Alternatively you can install the *Gradle Integration for Eclipse* plugin from the Eclipse Marketplace. It allows you to import Poodle directly as a Gradle project and recreate the project files from the project context menu etc.

In order to start Poodle in Eclipse, run the *main()* function in [Poodle.java](src/main/java/de/whs/poodle/Poodle.java).
