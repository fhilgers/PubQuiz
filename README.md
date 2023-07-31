# Pub Quiz
Project in 623.952 (23S) App Development at AAU Klagenfurt

By Group 7:
* Hilgers Felix
* Ismailov Ramiz


## Requirements

App is required to replace pen and paper on pub quiz sessions at company game night 
(that is, all players are located in the same room, quiz questions are displayed on the beamer).

### User Roles
Master - a game master who organizes the game night.
Player - a team of players who take part in the game night pub quiz.

### Main User Story

1. As a Master, I can set up a new pub quiz session, whereby I can define the following parameters:
   * Number of rounds
   * Number of questions per round
   * Timeout to complete the round
   * Number of answers per question
   * Time to answer the question
2. As a Master, I can prepare previously setup game.
3. As a Player, I can search for a new game and join it.
4. As a Master, I can see players joining the prepared game in lobby.
5. As a Master, I can start a new game as soon as I decide that all players are joined.
6. As a Player, I get notified that game is being started and I can report readiness, otherwise after timeout game starts anyway.
7. As a Master, I can see readiness of players
8. As a Master, I can start new round.
9. As a Player, I can see notification that new round is being started.
10. As a Master, I can start next question (timeout starts automatically)
11. As a Player, I can see that next question is started and following UI controls:
    * Possible answer (radio)buttons, e.g. (A), (B), (C), (D)
    * Remaining time indicator
    * Number of question from total in round, e.g. 2/5
12. As a Player, I can select the answer for current question
13. As a Master, I can see same screen as a player and select the correct answer
    * then I can see players answering status and I can
      * prolong the timeout
      * switch to next question (10)
      * force end of round (14)
14. As a Master, if all round questions are exhausted, I can trigger end of round (timeout starts automatically)
15. As a Player, I get notified that current round is about to end.
16. As a Player, I can submit the round.
17. As a Master, I can see players submitting the current round.
18. As a Master, I can trigger end of the game
19. As a Player, I can see notification that game is over
20. As a Master, I can see game statistics.

### Optional Features
* Game setup can be reused from history (1)
* Game setup can be imported from local file system or URL (1)
* Game setup can be extended to support customized number of questions and number of answers (1)
* Game setup can be extended to support preselected correct answers for questions
* Game setup can be extended to support labeled answers (1) which can be also displayed on user screen (11)
* Number of questions per round can be adjusted on the round start screen (8)
* Number of answers per question can be adjusted on the question start screen (10)

## Architecture & Design

In this project we use simplified [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html) approach.
That is, we the following layers:
* `domain` layer, which is platform- and framework-independent. It has no dependencies on any other layer.
* `providers` layer, which implements data, connectivity and other platform-specific services. 
   It has no dependencies on other layers but `domain` layer.
* `UI` layer, which implements user interface. It has dependencies on `domain` and `providers` layers.
* `MainActivity` is the entry point of the application and special layer that integrates other layers.

The application is based on Single Activity Architecture,
that is, all screens are implemented as a separate composable components linked together using Navigation.

### Technology Stack
* UI is based on the [Jetpack Compose](https://developer.android.com/jetpack/compose) framework
* Persistence is based on the [Room](https://developer.android.com/jetpack/androidx/releases/room) library
* Connectivity is based on the [Nearby Connections](https://developers.google.com/nearby/connections) service

### Testability
* `domain` layer is fully testable using pure unit tests
* `demo` layer contains pre-programmed mocks of network communication activities to test the application
   flow in "demo"-mode without real connected devices, i.e. in emulator or on a single device. 
