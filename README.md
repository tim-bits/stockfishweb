
<img align="right" src="https://stockfishchess.org/images/logo/icon_512x512@2x.png" height="300" width="300">


# StockfishWeb
**StockfishWeb** is high level wrapper a layer on top of [Stockfish](https://stockfishchess.org/), the world strongest UCI chess engine.
Implemented as a Spring Boot app, StockfishWeb exposes internal Stockfish functionality through REST API.
Also, additionally, it allows to validate chess positions represented by [FEN](https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation) string, and build your own [FEN](https://en.wikipedia.org/wiki/Forsyth%E2%80%93Edwards_Notation) string by positioning pieces on the virtual chess board.

## Prerequisites
To build and run StockfishWeb, maven (3.9+) and java (17+) are required.

## Building the app
```shell
	mvn package
```
or to speed things up by skipping all tests
```shell
	mvn -DskipTests package    
```
if there are no error during the build, you can proceed with

## Running the app
```shell
	mvn spring-boot:run
```
If you see a Spring logo with the Spring Boot version (i.e. 3.2.3) at the end of your console output, we can proceed with
#Using the app
In your browser, hit your localhost URL i.e.
```shell
http://localhost
```
and you should see the page below:

<img align="center" src="https://i.postimg.cc/L5PBPPZ9/chess-api.png" height="200" width="300">


Now you can do the following:
* Executing REST queries
* Validating FENs
* Building FENs from scratch or from existing FENs

#### Executing queries
Queries can be executed via the app front-end (FE) as well as via any http client capable of sending POST requests e.g. curl, postman etc

##### 1. FE queries:
   Either build a position on the virtual chess board or copy/paste a FEN into the FEN input field, choose a desired move depth in 'Depth' combo (maximum 15) and which side is to move in 'Side to move' selector, and then press 'Submit' button. 
   If the position is valid, 'Response' text area will be populated with a Json response in the following format:
```json
{
    "bestMove": "c4f7",
    "eval": "0.83",
    "continuation": "c4f7 e8f7 d1h5 g7g6 h5c5 d4d3 c5d4 g8f6 e4e5 f6d5 d4d3 d7d6 d3f3 f7g7 d2d4 h7h5 f1e1 h5h4 h2h3",
    "mate": ""
}
```
where  <br/>
```eval``` indicates position evaluation with minus sign indicating that position is in favor of black; <br/>
```mate``` indicates the number of moves till a forced checkmate, when applicable.

##### 1. Curl queries:
```shell
curl -X POST -H "Accept: application/json" -H "Content-Type:application/json" --data "{\"depth\":15, \"fen\": \"r1bqk1nr/p2p1ppp/2p5/1pb5/2BpP3/2P5/PP1P1PPP/RNBQ1RK1 w - - 0 1\"}" http://localhost
```
Just an fyi: maximum depth is defaulted to 15 i.e. any depth exceeding 15 will be truncated to 15 to avoid extremely long execution.  

### Acknowledgements

The FEN validation is reusing validation rules of [chess.js](https://github.com/jhlywa/chess.js) with addition of extra validation rules on top of it. <br/> The chess board is implemented using [chessboardjs](https://chessboardjs.com/).
<br/> Back end makes use of a revamped version of [Stockfish-Java](https://github.com/senyast4745/Stockfish-Java).

### License

[GNU General Public License V3](https://www.gnu.org/licenses/gpl-3.0.en.html) except for chess.js and chessboardjs which have their own licences on the top of their respective file headers.

### Dependencies
* [Stockfish](https://stockfishchess.org/)
