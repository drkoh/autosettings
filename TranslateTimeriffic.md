**IMPORTANT: This document is OBSOLETE**. Please read http://code.google.com/p/timeriffic/wiki/TranslateTimeriffic instead.

# Introduction #

Timeriffic is already translated in a number of languages, but we'd love to have more translations!

Ralfoide mantains the Timeriffic translations for English, French and Spanish.


This document explains what is involved.

# Details #

Before starting to translate anything, please contact us at
_**rdrr.labs gmail com**_ to make sure we don't already have a similar translation in progress.

You will need:
  * A text editor, or simply an email application.
  * You don't need any knowledge of how to develop for Android, nor any coding knowledge in fact.
  * The file to be edited is an XML file in Unicode UTF-8. It's fine if you have no clue what this means :-)

## Language ##

First you need to decide which language you can provide.
Languages in Android are codified using a 2-letter "**language code**", followed by an optional "**region code**".

Most of the time only the language code is needed, for example en=English, fr=French, etc.

==> Simply look at the
["ISO 639-1" column on this page](http://www.loc.gov/standards/iso639-2/php/code_list.php)
(the second column) and tell me which language code is best.

[Region codes](http://www.iso.org/iso/country_codes/iso_3166_code_lists/english_country_names_and_code_elements.htm) are rarely used, they only serve to differentiate two versions of the same language, for example US English (en-US) versus British (en-GB), so we can ignore that here.

## Translation ##

There are 2 XML files and 2 HTML files to translate.

The 2 XML files to translate are:
  * [res/values/strings.xml](http://code.google.com/p/autosettings/source/browse/trunk/AutoSettings/res/values/strings.xml) is the main file which contains all the visible user strings.
  * [res/values/arrays.xml](http://code.google.com/p/autosettings/source/browse/trunk/AutoSettings/res/values/arrays.xml) contains a few more strings used in the settings.

These are XML files. In these files, a typical line looks like this:
```
<string name="min_button_percent">Set Min to %d%%</string>
```

If you're not familiar with XML, this means:
  * A new string is being defined, it has a name and the value is the text between the `>...<` brackets.
  * The special notation `%d` means an integer value will be inserted here.
  * The special notation `%s` means a string will be inserted here.
  * The special notation `%%` will show as a single percentage.

So on-screen, that particular string will look like
```
Set Min to 42%
```


The 2 HTML files to translate are:
  * [assets/intro.html](http://code.google.com/p/autosettings/source/browse/trunk/AutoSettings/assets/intro.html) is the introduction that is displayed to users when they first start Timeriffic. It contains 3 sections: a) a brief introduction, b) a change log (what changed in each version) and c) a brief FAQ and credits. I would suggest you translate the introduction and the FAQ/credits and forget about the change log. The credit part is where you would give yourself credit for the translation.
  * [assets/error\_report.html](http://code.google.com/p/autosettings/source/browse/trunk/AutoSettings/assets/error_report.html) contains the help displayed when users use the _Menu > Report Error_ functionality.

All you have to do is copy the files' content in a text editor. Make sure to use an editor that has Unicode "UTF-8" support (Emacs, Eclipse, Visual Studio, UltraEdit, Notepad++, XCode, BBEdit, etc.) Translate the files as you think is best and email that to me at _**rdrr.labs gmail com**_. I'll create a new version of Timeriffic that you can try to see the translated text.