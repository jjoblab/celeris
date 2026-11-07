-- Corpus SQL — le registre des builds d'un éditeur embarquable

CREATE TABLE builds (
    id INTEGER PRIMARY KEY,
    module TEXT NOT NULL,
    version TEXT NOT NULL,
    duree_ms INTEGER DEFAULT 0,
    succes BOOLEAN NOT NULL DEFAULT 0,
    date TIMESTAMP
);

CREATE INDEX idx_builds_module ON builds(module);

CREATE VIEW resume AS
    SELECT module,
           COUNT(*) AS total,
           AVG(duree_ms) AS moyenne,
           MAX(duree_ms) AS pire
    FROM builds
    WHERE succes = 1
    GROUP BY module
    ORDER BY moyenne DESC
    LIMIT 10;

INSERT INTO builds (module, version, duree_ms, succes, date)
VALUES ('core-text', '1.2.0', 412, 1, '2026-11-06 09:30:00');

UPDATE builds
SET duree_ms = duree_ms + 50
WHERE module = 'core-syntax' AND version <> '1.0.0';

/* Le rapport mensuel : les modules lents d'abord,
   les échecs à part. */
SELECT b.module, b.version, b.duree_ms
FROM builds b
LEFT JOIN resume r ON r.module = b.module
WHERE b.duree_ms BETWEEN 100 AND 5000
    AND b.module LIKE 'core-%'
    AND b.id IS NOT NULL
UNION ALL
SELECT module, 'echec', 0 FROM builds WHERE succes = 0;

DELETE FROM builds WHERE date < '2026-01-01';
