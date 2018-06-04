TODO: 
====

play/backend:
-------------
* throttler -> stream
* remove minutesToView from BetterSettings; currently used for importer
* remove scalaz and use cats
* disable betting for users that canBet = false
* unit tests in backend for non-viewable bets  ????
* actor sending unsent mails
* view for unsent mails
* validate Excel in DBSpec
* multiple mail provider; when creating users or afterwards;

UI:
---
* create GAMES delayed (i.e. insert the time slot without the teams, then allow changing teams)
* unit test in javascript for betting component
* add individual games popup to stats view

IMPORTANT:
----------
* Recaptcha
* complete registration with token completeRegistration/123456789012345678901234567890123456
* fix urls in mail messages





