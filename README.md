# imt3673-lab2
IMT3673 Lab 2 - Simple RSS reader (Android, Java)

## The idea
Create an application that allows the user to read content from any RSS feed. The app will consist of 3 activities: one with the list of items (ListView, for selecting content), one for article content display (for reading content), and User Preferences (for user to specify the preferences). 

## Preferences
The user should be able to specify in the preferences the URL to the RSS feed (RSS2.0-based or Atom-based), and, the limiting number of items that should be displayed in the ListView (10, 20, 50, 100), and the frequency at which the app fetches the content (10min, 60min, once a day). The app will fetch the RSS feed and populate the list UP to the limit number. When user clicks on a particular item, a detailed view should be shown, with the content of the article for that item. 

## Checklist
* The git repository URL is correctly provided, such that command works: git clone <url>
* The code is well, logically organised and structured into appropriate classes. Everything should be in a single package.
* It is clear to the user what RSS feed formats are supported (RSS2.0 and/or Atom)
* The user can go to Preferences and set the URL of the RSS feed.
* The user can go to Preferences and set the feed item limit.
* The user can go to Preferences and set the feed refresh frequency.
* The user can see the list of items from the feed on the home Activity ListView.
* The user can go to a particular item by clicking on it. The content will be displayed in newly open activity. The back button puts the user back onto the main ListView activity to select another item. 
* The user can press the back button from the main activity to quit the app. 
* When the content article has graphics, it is rendered correctly. 

## Hints
Make sure that the logic for the fetching of articles is done by the app automatically with the frequency given by the user Preferences. How would you schedule it? How would you prefetch the articles? For testing purposes, add a button to the main activity (the one with the list), to FORCE a fetch upon press of the button. Final app should not have the "fetch" button.

The content of the article should use WebView such that graphics of the content article is rendered correctly, if the content was an URL. If the content was a plain text, a simple text view can be used.

Make sure you use appropriate facilities (a library?) to help you parsing XML content. Should the user specify if the feed is in one format or another, or can the app detect it automatically? Can you use a library that parses RSS2.0 and Atom? What would be the benefit? What would be the limitation?
