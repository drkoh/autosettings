# Introduction #

Brighteriffic is currently translated in English, French and Spanish.

I'd love to have more translations.

This document explains what is involved.

# Details #

Before starting to translate anything, please contact me at
_**ralfoide gmail com**_ to make sure I don't already have a similar translation in progress.

You will need:
  * A text editor, or simply an email application.
  * You don't need any knowledge of how to develop for Android, nor any coding knowledge in fact.
  * The file to be edited is an XML file. It's fine if you have no clue what this means :-)

## Language ##

First you need to decide which language you can provide.
Languages in Android are codified using a 2-letter "**language code**", followed by an optional "**region code**".

Most of the time only the language code is needed, for example en=English, fr=French, etc.

==> Simply look at the
["ISO 639-1" column on this page](http://www.loc.gov/standards/iso639-2/php/code_list.php)
(the second column) and tell me which language code is best.

[Region codes](http://www.iso.org/iso/country_codes/iso_3166_code_lists/english_country_names_and_code_elements.htm) are rarely used, they only serve to differentiate two versions of the same language, for example US English (en-US) versus British (en-GB), so we can ignore that here.

## Translation ##

The original text to translate is located at
[res/values/strings.xml](http://code.google.com/p/autosettings/source/browse/trunk/Brighteriffic/res/values/strings.xml) on this site.

A typical line looks like this:
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

All you have to do is copy the file's content in a text editor or email editor, translate it as you think is best and email that to me at _**ralfoide gmail com**_.