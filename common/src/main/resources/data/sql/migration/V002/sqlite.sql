-- SQLite Conversion from V1 -> V2; Part 0

-- Create new tunnel table

ALTER TABLE journey_tunnels RENAME TO journey_tunnels_legacy;

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

INSERT INTO journey_tunnels (
    SELECT origin_domain_id,
    origin_x, origin_y, origin_z,
    origin_x, origin_y, origin_z,
    destination_domain_id,
    destination_x, destination_y, destination_z,
    tunnel_type
);

DROP TABLE journey_tunnels_legacy;