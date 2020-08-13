READ ME
Authors: Reuben Rappaport (rbr76) and Alex Clark (ayc55)

INSTRUCTIONS
Install java 8 by running

sudo apt-get install openjdk-8-jdk

Then run the provided jar by running

java -jar Classify.jar password

Where password is the password you want to classify

NOTE: entering passwords with characters like !, $, & as non-final characters in password
may lead to issues, as these are characters that need to be escaped in sh or bash.
The easiest way to prevent issues of this sort is entering such passwords enveloped in single quotes (double quotes work to protect some symbols but NOT all), which will make all characters “safe” except single quote characters which are part of the password.
Escaping any potentially problematic symbol characters using backslashes will also prevent the issue.


RECIPES:
The password recipes we implemented were basic16 and comprehensive8, the latter of which references the inbuilt Unix dictionary. 
If an entered password passes either basic16 or comprehensive8, it is classified as strong, otherwise it is classified as weak.