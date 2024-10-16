BEGIN;

-- Journey Waypoints

CREATE TABLE journey_waypoints (
    player_uuid BINARY(16),
    name_id     VARCHAR(255)    NOT NULL,
    name        VARCHAR(255)    NOT NULL,
    domain_id   BINARY(16)      NOT NULL,
    x           INT             NOT NULL,
    y           INT             NOT NULL,
    z           INT             NOT NULL,
    created     TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    publicity   INT             NOT NULL    DEFAULT 0,
    UNIQUE      (player_uuid, name_id)
);
CREATE INDEX journey_waypoints_player_uuid_idx ON journey_waypoints (player_uuid);
CREATE INDEX journey_waypoints_name_id_idx ON journey_waypoints (name_id);

-- Journey Path Cache

CREATE TABLE journey_cached_paths (
    id              INTEGER         NOT NULL    PRIMARY KEY AUTOINCREMENT,
    created         TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    duration        INT             NOT NULL,
    path_length     DOUBLE(12, 5)   NOT NULL,
    origin_x        INT             NOT NULL,
    origin_y        INT             NOT NULL,
    origin_z        INT             NOT NULL,
    destination_x   INT             NOT NULL,
    destination_y   INT             NOT NULL,
    destination_z   INT             NOT NULL,
    domain_id       BINARY(16)      NOT NULL
);
CREATE INDEX journey_cached_paths_idx ON journey_cached_paths (
	origin_x, origin_y, origin_z,
	destination_x, destination_y, destination_z,
	domain_id
);

CREATE TABLE journey_cached_path_cells (
    path_id         INTEGER     NOT NULL,
    x               INT         NOT NULL,
    y               INT         NOT NULL,
    z               INT         NOT NULL,
    path_index      INT         NOT NULL,
    mode_type       SMALLINT    NOT NULL,
    FOREIGN KEY (path_id)
        REFERENCES  journey_cached_paths (id)
        ON DELETE   CASCADE
        ON UPDATE   CASCADE,
    UNIQUE  (path_id, path_index)
);
CREATE INDEX journey_cached_path_cells_path_id_idx ON journey_cached_path_cells (path_id);

CREATE TABLE journey_cached_path_modes (
    path_id         INTEGER   NOT NULL,
    mode_type       SMALLINT  NOT NULL,
    FOREIGN KEY     (path_id)
        REFERENCES  journey_cached_paths (id)
        ON DELETE   CASCADE
        ON UPDATE   CASCADE,
    UNIQUE  (path_id, mode_type)
);
CREATE INDEX journey_cached_path_modes_path_id_idx ON journey_cached_path_modes (path_id);

-- Journey Tunnel Cache

CREATE TABLE journey_tunnels (
    entrance_domain_id  BINARY(16)  NOT NULL,
    entrance_0_x        INT         NOT NULL,
    entrance_0_y        INT         NOT NULL,
    entrance_0_z        INT         NOT NULL,
    entrance_1_x        INT         NOT NULL,
    entrance_1_y        INT         NOT NULL,
    entrance_1_z        INT         NOT NULL,
    exit_domain_id      BINARY(16)  NOT NULL,
    exit_x              INT         NOT NULL,
    exit_y              INT         NOT NULL,
    exit_z              INT         NOT NULL,
    tunnel_type         TINYINT     NOT NULL
);
CREATE INDEX journey_tunnels_entrance_idx on journey_tunnels (
        entrance_domain_id,
        entrance_0_x, entrance_0_y, entrance_0_z,
        entrance_1_x, entrance_1_y, entrance_1_z
);
CREATE INDEX journey_tunnels_exit_idx on journey_tunnels (
        exit_domain_id, exit_x, exit_y, exit_z
);

-- Journey Database Version Tracker

CREATE TABLE journey_db_version (
    db_version INT
);
CREATE INDEX journey_db_version_idx ON journey_db_version (
    db_version
);

COMMIT;