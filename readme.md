# Bulk Google Translate Chrome Extension
This extension translates one to many languages in bulk. You'll need to supply it with a csv file, and you'll be able to download all the translated results at the end of the run in json format. On top of that, it also downloads the audio file in the source language automatically to your default chrome downloads directory.

## Installation
1. Install Java
2. Install [leiningen](http://leiningen.org).
3. Either `git clone git@github.com:noitcudni/bulk-google-translate.git` or download the zip file from [github](https://github.com/noitcudni/bulk-google-translate/archive/master.zip) and unzip it.
4. `cd` into the project root directory.
  * Run in the terminal
  ```bash
  lein release && lein package
  ```
5. Go to **chrome://extensions/** and turn on Developer mode.
6. Click on **Load unpacked extension . . .** and load the extension. You will find the compiled js files in the `releases` folder.

## Usage
1. Create a list of words that you'd like to be translated in a csv file.
2. Visit https://translate.google.com/
3. Click on the extension popup icon.
4. Choose the source language or let google auto detect.
5. Choose the target languages.
6. Select your CSV file created in step 1.
7. Download the translated results in JSON format.
