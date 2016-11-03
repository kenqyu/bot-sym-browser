##SymBrowser

###Created by Ryan D'souza


####Purpose
This bot allows you to browse the Internet and certain “Contexts”, while making it look like you’re doing work.
Right now, only the Reddit context is allowed, but one can get the text from any website in a format that looks like it is a long message from another Symphony user.
Other contexts can be implemented fairly easily through the bot’s modular design.


####Modification Instructions

Navigate to [symbrowser.properties](https://github.com/symphonyoss/bot-sym-browser/blob/master/src/resources/symbrowser.properties) under "bot-sym-browser/src/resources" and modify all custom fields that are indicated with capital letters inside "[]". Add your Symphony-specific ".p12" certificate file to this "resources" directory as well.


####Run Instructions

- Download this ZIP 

- Uncompress this ZIP 

- Open project folder in IntelliJ 

- Install maven dependencies from pom.xml

- Modify [symbrowser.properties](https://github.com/symphonyoss/bot-sym-browser/blob/master/src/resources/symbrowser.properties) (filepath: "bot-sym-browser/src/resources/symbrowser.properties") with custom URLs and API Keys) 

- Make sure to add your Symphony certificate file inside the "resources" directory ("bot-symbrowser/src/resources/") with "symphony-bot-user.p12" as the file name

- Run the main method of 'SymBrowserBot.java'

