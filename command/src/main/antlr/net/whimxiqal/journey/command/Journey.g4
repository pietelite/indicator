grammar Journey;

journey: JOURNEY (setwaypoint | listwaypoints | waypoint | player | server | admin | cancel)? EOF;
journeyto: JOURNEY_TO journeytoTarget? EOF;

setwaypoint: SET_WAYPOINT name=identifier+;
listwaypoints: LIST_WAYPOINTS (listwaypointsMine | listwaypointsPlayer);
listwaypointsMine: page=ID?;
listwaypointsPlayer: player user=identifier page=ID?;
waypoint: WAYPOINT name=identifier (unsetWaypoint | renameWaypoint | publicWaypoint | flagSet)?;
unsetWaypoint: UNSET;
renameWaypoint: RENAME newname=identifier;
publicWaypoint: PUBLIC (TRUE | FALSE)?;

player: PLAYER user=identifier (playerWaypoint)?;
playerWaypoint: name=identifier;

server: SERVER (serverSetWaypoint | serverListWaypoints | serverWaypoint);
serverSetWaypoint: SET_WAYPOINT name=identifier;
serverListWaypoints: LIST_WAYPOINTS page=ID?;
serverWaypoint: WAYPOINT name=identifier (serverUnsetWaypoint | serverRenameWaypoint | flagSet)?;
serverUnsetWaypoint: UNSET;
serverRenameWaypoint: RENAME newname=identifier;

admin: ADMIN (debug | cache | reload=RELOAD | listNetherPortals);
debug: DEBUG;
cache: CACHE (cachePortals | cachePaths);
cachePortals: PORTALS (clear=CLEAR);
cachePaths: PATHS (clear=CLEAR | build=BUILD);
listNetherPortals: LIST_NETHER_PORTALS page=ID?;

cancel: CANCEL;

journeytoTarget: name=identifier flagSet?;

flagSet: (timeoutFlag | animateFlag | flyFlag | doorFlag | digFlag | navigatorFlag)+;
timeoutFlag: FLAG_TIMEOUT timeout=ID;
animateFlag: FLAG_ANIMATE (delay=ID)?;
flyFlag: FLAG_FLY (TRUE | FALSE)?;
doorFlag: FLAG_DOOR (TRUE | FALSE)?;
digFlag: FLAG_DIG (TRUE | FALSE)?;
navigatorFlag: FLAG_NAVIGATOR navigator=identifier (OPEN_BRACE options=identifier CLOSED_BRACE)?;

ADMIN: 'admin';
BUILD: 'build';
CACHE: 'cache';
CANCEL: 'cancel';
CLEAR: 'clear';
COLON: ':';
DEBUG: 'debug';
FALSE: 'false';
INVALIDATE: 'invalidate';
JOURNEY: 'journey';
JOURNEY_TO: 'journeyto';
LIST_NETHER_PORTALS: 'listnetherportals';
LIST_WAYPOINTS: 'listwaypoints';
PATH: 'path';
PATHS: 'paths';
PLAYER: 'player';
PORTALS: 'portals';
PUBLIC: 'public';
RELOAD: 'reload';
RENAME: 'rename';
SERVER: 'server';
SET: 'set';
SET_WAYPOINT: 'setwaypoint';
TRUE: 'true';
UNSET: 'unset';
WAYPOINT: 'waypoint';

FLAG_ANIMATE: '-animate';
FLAG_DIG: '-dig';
FLAG_DOOR: '-door';
FLAG_FLY: '-fly';
FLAG_NAVIGATOR: '-navigator';
FLAG_TIMEOUT: '-timeout';

OPEN_BRACE: '{';
CLOSED_BRACE: '}';

// MANTLE NODES
identifier: ident | SINGLE_QUOTE ident+ SINGLE_QUOTE | DOUBLE_QUOTE ident+ DOUBLE_QUOTE;
ident: ID
        | ADMIN
        | BUILD
        | CACHE
        | CANCEL
        | CLEAR
        | DEBUG
        | FALSE
        | INVALIDATE
        | JOURNEY
        | JOURNEY_TO
        | LIST_NETHER_PORTALS
        | LIST_WAYPOINTS
        | PATH
        | PLAYER
        | PUBLIC
        | RELOAD
        | RENAME
        | SERVER
        | SET
        | SET_WAYPOINT
        | TRUE
        | UNSET
        | WAYPOINT;
ID: [a-zA-Z0-9\-_:,.]+;
SINGLE_QUOTE: '\'';
DOUBLE_QUOTE: '"';
WS : [ \t\r\n]+ -> channel(HIDDEN); // skip spaces, tabs, newlines
// END MANTLE NODES