# NONSENSE-GENERATOR PROJECT MANUAL

## INTRODUCTION

**Nonsense Generator** is a Java application that uses the Google Cloud Natural Language API to analyze the grammatical structure of sentences entered by the user. The goal is to generate and analyze syntactically correct but nonsensical sentences, with nouns, verbs, and adjectives randomly selected from a dedicated set and inserted into randomly chosen templates.

## REQUIREMENTS

### SOFTWARE

- Java Development Kit (JDK) 21
- Google Cloud SDK (including the authentication JSON file to be placed in the [config](../config) folder,
  it's importanto to rename the .json file as follows: "credentials.json", otherwise the connection won't work)
- Internet connection for using the Google API
- **Apache Maven ≥ 3.9** for dependency management and project compilation (see [pom](../pom.xml))

## STARTING THE APPLICATION
Open a terminal and type:
```
git clone https://github.com/sclosaf/NonSense-Generator.git
cd NonSense-Generator
```
Otherwise download the .zip from the repo home and unzip it locally.
To streamline the development and usage process, a series of scripts have been provided, see [script](../scripts), these script are available to the user and perform the same actions: 
- [mvn.sh](../scripts/mvn.sh): UNIX/LINUX
- [mvn.bat](../scripts/mvn.bat): Windows

These provide the basic compilation and execution functionalities, in order to compile and start the
application the following instructions.
On Unix-like environment use:
```
cd scripts
./mvn.sh clean # In order to clean the development environment
./mvn.sh package # In order to perform a combination of compilation, testing and documentation
./mvn.sh execute # In order to execute the .jar application
```
Otherwise, it'e equivalent:
```
cd scripts
./mvn.sh all # Performs the whole compilation and execution process
```
On Windows environment use:
```
cd scripts
.\mvn.bat clean # In order to clean the development environment
.\mvn.bat package # In order to perform a combination of compilation, testing and documentation
.\mvn.bat execute # In order to execute the .jar application
```
Otherwise, it'e equivalent:
```
cd scripts
.\mvn.bat all # Performs the whole compilation and execution process
```
Anyway the two scripts can perform the individual operations, type `./mvn.sh` or `.\mvn.bat` to see the available options.

As an alternative compilation and execution commands can be performed autonomously directly from the root of the project.

**WARNING** The first installation process performed on a new environment may produce verbose output in
the terminal due to dependency downloads and initialization processes, every dependency is installed in 
the newly created lib folder, leaving clean the global directory that Maven generally uses, this is a 
choice to make the project self contained and autonomous.

## INTERFACE

### MAIN MENU OPTIONS

Once the application is executed, a menu with the following options will be displayed:
- **Default**
- **Personalized**
- **Generate**
- **Analyze**
- **Tree**
- **Extend**
- **Set tolerance**
- **Help**
- **Info**
- **Clear**
- **Quit**

### HOW THE CODE WORKS
The application provides a basic CLI for the user, into which execute the selected command (or its
shortcut), once the command is chosen and typed press Enter (the following informations will be available 
with the command info, once into the application):

- **Default** 
  Performs a basic but complete procedure of the functionalities offered, this procedure offers the
  possibility to generate and/or analyze a sentence and ends with the syntactic tree of the sentence being
  displayed.
  This is a default combination of commands, for a more specific combination use 'personalized'; the other
  entries perform singular functionalities.

- **Personalized**
  Performs, in a specific order, all the functionalities offered by the program.
  In each step a full personalization of the commands is available, the user can choose every modality and functionality for each process.

- **Generate**
  Generates a random nonsense sentence, based on a pool of various templates, nouns, adjectives and verbs, which are randomly combined.
  The sentence, although grammatically correct, totally lacks logical sense, as the combination is completely random.
  The sentence is printed and cached for future analysis, only the last generated sentence is cached, any previous is overwritten; the program begins without any cached sentence.

- **Analyze**
  Offers a number of analysis procedures which can be used in various combinations, as the analysis can be chosen by the user, be random, costumized or all of the above.
  The types of analysis offered are ones regarding the syntax, the sentiment, the toxicity or the entities that are contained in the sentence.

- **Tree**
  Proceeds to print the syntactic tree of the chosen sentence.
  Shows the hierarchical structure of the sentence and the relationship among its components.
  This functionality can support the analysis of more than one sentence.
  This function requires the execution of the syntax analysis of the sentence in background.
  It is implicit that this process requires the sentence to be at least grammatically correct.

- **Extend**
  Proceeds to let the user extend the dictionaries used by the program to generate the sentence; in particular the user can input nouns, adjectives or verbs at will.
  This update is immediately applied to the program, but doesn't last among different sessions.

- **Set tolerance**
  Allows the user to change the program's tolerance when considering an analyzed sentence via toxicity analysis, setting the upper bound over which a sentence
  is considered to be toxic by the program.
  The default value is setted to 0.7 (it ranges from 0.0 to 1.0, inclusive).

- **Help**
  Displays basic help information, for commands and their purpose.

- **Info**
  Shows detailed information about commands and their shortcuts.
  Provides extended help for each available command (including hidden ones).
  Fuction, use and purpose are explained in detail for each command.

- **Verbose**
  Hidden command that toggles verbose output mode. When enabled, it provides a more detailed feedback about the background activity performed by the program.
  Used for debugging purposes, its use is not recommended for a basic user experience, as it can cause much 'noise' on the terminal. 
  The default setting for this function is off.

- **Clear**
  Clears the terminal screen.
  Resets the display and shows the title and the initial menu.
  Helpful when there is too much output on the terminal.

- **Quit**
  Exits the program.
  Terminates the application safely.

During Runtime the code produces a folder too, called logs, which contains the most recent logs for the project
reporting its activity

## PROJECT STRUCTURE

- **src/**
  - **main/**
    - **java\unipd\nonsense/**
      - **analyzer/**
        - SentenceAnalyzer.java
        - ToxicityValidator.java
      - **exceptions/**
        - ClientAlreadyClosedException
        - ClientNonExistentException
        - FailedAnalysisException
        - FailedClientInitializationException
        - FailedOpeningInputStreamException
        - IllegalToleranceException
        - InaccessibleFileException
        - InvalidFilePathException
        - InvalidGrammaticalElementException
        - InvalidJsonKeyException
        - InvalidJsonStateException
        - InvalidListException
        - InvalidNumberException
        - InvalidTemplateException
        - InvalidTemplateTypeException
        - InvalidTenseException
        - InvalidTextException
        - InvalidThresholdException
        - JsonElementIsNotArrayException
        - JsonElementIsNotPrimitiveException
        - MissingInternetConnectionException
        - NullClientException
        - NullFilePathException
        - NullJsonKeyException
        - NullLoggerException
        - SentenceNotCachedException
        - UnreadableFileException
        - UnwritableFileException
      - **generator/**
        - RandomAdjectiveGenerator.java
        - RandomNounGenerator.java
        - RandomTemplateGenerator.java
        - RandomVerbGenerator.java
        - SentenceGenerator.java
        - SyntaxTreeBuilder.java
      - **model/**
        - Adjective.java
        - Noun.java
        - Number.java
        - Pair.java
        - Placeholder.java
        - SyntaxToken.java
        - Template.java
        - Tense.java
        - Verb.java
      - **util/**
        - CLI.java
        - CommandProcessor.java
        - GoogleApiClient.java
        - JsonFileHandler.java
        - JsonUpdateObserver.java
        - JsonUpdater.java
        - LoggerManager.java
      - App.java
    - **resources/**
      - adjective.json
      - nouns.json
      - templates.json
      - verbs.json
  - **test/**
    - **java\unipd\nonsense/**
      - **analyzer/**
        - TestSentenceAnalyzer.java
        - TestToxicityValidator.java
      - **generator/**
        - TestRandomAdjectiveGenerator.java
        - TestRandomNounGenerator.java
        - TestRandomTemplateGenerator.java
        - TestRandomVerbGenerator.java
        - TestSentenceGenerator.java
        - TestSyntaxTreeBuilder.java
      - **model/**
        - TestAdjective.java
        - TestNoun.java
        - TestSyntaxToken.java
        - TestTemplate.java
        - TestVerb.java
      - **util/**
        - TestCLI.java
        - TestGoogleApiClient.java
        - TestJsonFileHandler.java
        - TestJsonUpdater.java
        - TestLoggerManager.java
    - **resources/**
      - testAdjective.json
      - testConfig.json
      - testConfigAlternative.json
      - testNouns.json
      - testSentence.json
      - testTemplates.json
      - testToxicity.json
      - testVerbs.json
- **pom.xml**
- **README.md**

### EXTERNAL LIBRARIES

- **JUnit Jupiter** (org.junit.jupiter:junit-jupiter-api, org.junit.jupiter:junit-jupiter-engine):  
  Used for unit testing, in the (src/test)[src] section all the testing classes can be found.
  There is a Maven PlugIn related: **Surefire**, which automaticaly produces the tests repors, visualizing
  successfull tests and time spent for each test

- **Google Cloud Language** (com.google.cloud:google-cloud-language):  
  Used for natural language analysis, through which the chosen sentences can be analysed, requires API
  key, it's important to remember to insert the owned service account key, in .json format.

- **Gson** (com.google.code.gson:gson):  
  Used for JSON handling through a dedicated interface class, can be found in [util](/src/main/java/unipd/nonsense/util) folder.

- **Log4j** (org.apache.logging.log4j:log4j-core, org.apache.logging.log4j:log4j-api):  
  Used for logging through a dedicated interface class, can be found in [util](/src/main/java/unipd/nonsense/util) folder.

- **Disruptor** (com.lmax:disruptor):  
  Used for high-performance and async logging (with Log4j), there is no direct code that uses it, but it's a basic support for the logger.

- **Mockito** (org.mockito:mockito-core, org.mockito:mockito-junit-jupiter):  
  Used for mocking classes and instances in tests through reflections, mainly, in order to test a wider set of instances that may occur in the code.

- **JLine** (org.jline:jline):  
  Used for advanced console/CLI management to grant a user-friendly interface and commands management, similar to a classical shell.
 
### '.json' FILES
These files are used by the programm as dictionaries, for read and write operations:
- adjective.json
- nouns.json
- templates.json
- verbs.json
