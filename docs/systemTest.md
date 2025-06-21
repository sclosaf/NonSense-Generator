# System test
## User story 1
### As a user, I want to input a sentence so that the system can analyze and extract its grammatical components.
 For example, the sentence: "The cat is on the table".
Acceptance criteria:
* The system parses the sentence. For example "The", "cat", "is", "on", "the", "table".
- Sentence correctly parsed
* The system extracts and lists all nouns, verbs, and adjectives. For example "cat" - noun, "is" - verb, "table" - noun.
- All components are listed and defined correctly
* The output is displayed to the user in a readable format. For example the components are displayed one after the other with each analysys. 
- The output is correclty displayed.

## User story 2
### As a user, I want the system to generate a random sentence based on a template so I can get a nonsense result.
Acceptance criteria:
* The sentence template is selected or randomly generated. For example "The [adjective] [noun] [verb] near the [noun]."
- By using the personalized command, five sentence templates are correctly generated and can be selected. 
* The sentence is populated with words from the internal dictionaries. For example "The spare fruits balance near the things."
- Sentence correctly completed.
* The output is grammatically correct and displayed to the user.
- Sentence correctly displayed.

## User story 3
### As a user, I want my input sentence’s words to be reused so the system adapts over time.
 For example, the sentence "The cat is on the table"
Acceptance criteria:
* The system parses the input sentence.
- The sentence is correctly parsed as seen in user story 1.
* New words are extracted and appended to the appropriate file. For example a json list.
- The json files are correctly updated.
* The next time, the new words are available for random generation.
- The generated sentences pick elements from the json files that now contain the newly inserted words as well.

## User story 4
### As a user, I want to validate the toxicity of the generated sentence so that I can ensure it is safe.
 For example, the sentence "No joint wall named this point like that."
Acceptance criteria:
* The generated sentence is sent to the moderation API.
- The sentence is correctly given to the API.
* The toxicity score/result is returned and displayed.
- The sentence is analyzed through many parameters, a percentage is displayed for each.
* A message is shown if the sentence is flagged as inappropriate.
- If the sentence is deemed inappropriate, the following message is displayed: "Overall Assessment: TEXT FLAGGED AS POTENTIALLY INAPPROPRIATE"

## User story 5
### As a user, I want to accurately understand which functionalities the program is capable of.
Acceptance criteria:
* The system grants a series of commands.
- Correct.
* Information is displayed on screen defining the specifics of each command.
- Commands and specifics are printed on console at the start of execution.
* The user can access information about the system at any given time.
- Information access is granted at any time by the "help" and "info" commands.
