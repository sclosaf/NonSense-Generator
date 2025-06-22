# NONSENSE-GENERATOR PROJECT MANUAL

## INTRODUCTION

**Nonsense Generator** is a Java application that uses the Google Cloud Natural Language API to analyze the grammatical structure of sentences entered by the user. The goal is to create and analyze syntactically correct but nonsensical sentences, with nouns, verbs, and adjectives randomly selected from a dedicated set and inserted into randomly chosen templates.

## REQUIREMENTS

### SOFTWARE

- Java Development Kit (JDK) 21
- Google Cloud SDK (including the authentication JSON file to be placed in the **config** folder)
- Internet connection for using the Google API
- **Apache Maven** for dependency management and project compilation

## STARTING THE APPLICATION

1. Open the **NonSense-Generator** folder
2. Open the script folder
3. Open a terminal

On Windows:  
4. Enter ".\mvn.bat clean" and press enter  
5. Enter ".\mvn.bat package" and press enter  
6. Enter ".\mvn.bat execute" and press enter to run the program  

On Linux:  
4. Enter "./mvn.sh clean" and press enter  
5. Enter "./mvn.sh package" and press enter  
6. Enter "./mvn.sh execute" and press enter to run the program  

## INTERFACE

### MAIN MENU OPTIONS

Once the application is started, a menu with the following options will be displayed:

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

To select the desired option, type its name or initial and press enter.

- **Default** 
  Performs a basic but complete procedure of the functionalities offered, this procedure offers the possibility to generate and/or analyze a sentence and ends with the syntactic tree of the sentence being displayed.
  This is a default combination of commands, for a more specific combination use 'personalized'; the other entries perform singular functionalities.

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
  Allows the user to change the program's tolerance when considering an analyzed sentence via toxicity analysis, setting the upper bound over which a sentence is considered to be toxic by the program.
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
  for unit testing.

- **Google Cloud Language** (com.google.cloud:google-cloud-language):  
  for natural language analysis.

- **Gson** (com.google.code.gson:gson):  
  for JSON handling.

- **Log4j** (org.apache.logging.log4j:log4j-core, org.apache.logging.log4j:log4j-api):  
  for logging.

- **Disruptor** (com.lmax:disruptor):  
  for high-performance concurrent data structures.

- **Mockito** (org.mockito:mockito-core, org.mockito:mockito-junit-jupiter):  
  for mocking in tests.

- **JLine** (org.jline:jline):  
  for advanced console/CLI management.

### '.json' FILES

- adjective.json
- nouns.json
- templates.json
- verbs.json

## MEMBERS

- **Surname and Name**: Baldo Francesco, Ceretti Fabio, Lamparelli Davide, Sclosa Fabrizio
- **Course**: Elementi di Ingegneria del Software  
- **Academic year**: 2024/2025  
- **Professor**: Prof. Luca Boldrin