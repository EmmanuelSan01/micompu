DROP DATABASE IF EXISTS mi_compu_unab;
CREATE DATABASE mi_compu_unab
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE mi_compu_unab;

CREATE TABLE rol (
  id_rol INT         AUTO_INCREMENT,
  nombre VARCHAR(16) NOT NULL,
  estado BOOLEAN     NOT NULL DEFAULT TRUE,

  CONSTRAINT pk_rol PRIMARY KEY (id_rol)
);

CREATE TABLE usuario (
  id_usuario    INT          AUTO_INCREMENT,
  id_rol        INT          NOT NULL,
  email         VARCHAR(64)  NOT NULL UNIQUE,
  nombre        VARCHAR(128) NOT NULL,
  password_hash VARCHAR(64)  NOT NULL,
  estado        BOOLEAN      NOT NULL DEFAULT TRUE,

  CONSTRAINT pk_usuario PRIMARY KEY (id_usuario),

  CONSTRAINT fk_usuario_rol
    FOREIGN KEY (id_rol) REFERENCES rol(id_rol)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);

CREATE TABLE sala (
  id_sala        INT         AUTO_INCREMENT,
  id_responsable INT         NOT NULL,
  nombre         VARCHAR(16) NOT NULL UNIQUE,
  capacidad      INT         NOT NULL,
  estado         BOOLEAN     NOT NULL DEFAULT TRUE,

  CONSTRAINT pk_sala PRIMARY KEY (id_sala),

  CONSTRAINT fk_sala_responsable
    FOREIGN KEY (id_responsable) REFERENCES usuario(id_usuario)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);

CREATE TABLE equipo (
  id_equipo INT     AUTO_INCREMENT,
  id_sala   INT     NOT NULL,
  estado    BOOLEAN NOT NULL DEFAULT TRUE,

  CONSTRAINT pk_equipo PRIMARY KEY (id_equipo),

  CONSTRAINT fk_equipo_sala
    FOREIGN KEY (id_sala) REFERENCES sala(id_sala)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);

CREATE TABLE reserva (
  id_reserva  INT  AUTO_INCREMENT,
  id_usuario  INT  NOT NULL,
  id_equipo   INT  NOT NULL,
  fecha       DATE NOT NULL,
  hora_inicio TIME NOT NULL,
  hora_fin    TIME NOT NULL,

  CONSTRAINT pk_reserva PRIMARY KEY (id_reserva),

  CONSTRAINT fk_reserva_usuario
    FOREIGN KEY (id_usuario) REFERENCES usuario(id_usuario)
    ON UPDATE CASCADE
    ON DELETE RESTRICT,

  CONSTRAINT fk_reserva_equipo
    FOREIGN KEY (id_equipo) REFERENCES equipo(id_equipo)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);

CREATE TABLE franja_clase (
  id_franja_clase INT              AUTO_INCREMENT,
  id_sala         INT              NOT NULL,
  dia_semana      TINYINT UNSIGNED NOT NULL,
  hora_inicio     TIME             NOT NULL,
  hora_fin        TIME             NOT NULL,
  motivo          VARCHAR(64)      NOT NULL,
  estado          BOOLEAN          NOT NULL DEFAULT TRUE,

  CONSTRAINT pk_franja_clase PRIMARY KEY (id_franja_clase),

  CONSTRAINT fk_franja_clase_sala
    FOREIGN KEY (id_sala) REFERENCES sala(id_sala)
    ON UPDATE CASCADE
    ON DELETE RESTRICT
);