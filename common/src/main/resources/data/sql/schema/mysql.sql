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
    publicity   BIT             NOT NULL    DEFAULT 0,
    INDEX       (player_uuid),
    INDEX       (name_id),
    INDEX       location_idx    (domain_id, x, y, z),
    UNIQUE      (player_uuid, name_id)
);

-- Journey Path Cache

CREATE TABLE journey_cached_paths (
    id              INT             NOT NULL    PRIMARY KEY AUTO_INCREMENT,
    created         TIMESTAMP       NOT NULL    DEFAULT CURRENT_TIMESTAMP,
    duration        INT             NOT NULL,
    path_length     DOUBLE(12, 5)   NOT NULL,
    origin_x        INT             NOT NULL,
    origin_y        INT             NOT NULL,
    origin_z        INT             NOT NULL,
    destination_x   INT             NOT NULL,
    destination_y   INT             NOT NULL,
    destination_z   INT             NOT NULL,
    domain_id       BINARY(16)      NOT NULL,
    INDEX journey_cached_paths_idx (
        origin_x, origin_y, origin_z,
        destination_x, destination_y, destination_z,
        domain_id
    )
);

CREATE TABLE journey_cached_path_cells (
    path_id         INT         NOT NULL,
    x               INT         NOT NULL,
    y               INT         NOT NULL,
    z               INT         NOT NULL,
    path_index      INT         NOT NULL,
    mode_type       SMALLINT    NOT NULL,
    FOREIGN KEY (path_id)
        REFERENCES  journey_cached_paths (id)
        ON DELETE   CASCADE
        ON UPDATE   CASCADE,
    UNIQUE  (path_id, path_index),
    INDEX   (path_id)
);

CREATE TABLE journey_cached_path_modes (
    path_id         INT         NOT NULL,
    mode_type       SMALLINT  NOT NULL,
    FOREIGN KEY     (path_id)
        REFERENCES  journey_cached_paths (id)
        ON DELETE   CASCADE
        ON UPDATE   CASCADE,
    UNIQUE  (path_id, mode_type),
    INDEX   (path_id)
);

-- Journey Tunnel Cache

CREATE TABLE journey_tunnels (
    origin_domain_id        BINARY(16)  NOT NULL,
    origin_x                INT         NOT NULL,
    origin_y                INT         NOT NULL,
    origin_z                INT         NOT NULL,
    destination_domain_id   BINARY(16)  NOT NULL,
    destination_x           INT         NOT NULL,
    destination_y           INT         NOT NULL,
    destination_z           INT         NOT NULL,
    tunnel_type             TINYINT     NOT NULL,
    INDEX journey_tunnels_origin_idx (
        origin_domain_id, origin_x, origin_y, origin_z
    ),
    INDEX journey_tunnels_destination_idx (
        destination_domain_id, destination_x, destination_y, destination_z
    )
);

COMMIT;
