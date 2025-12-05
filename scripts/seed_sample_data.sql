START TRANSACTION;
SET @now := NOW();
SET @d7  := DATE_SUB(@now, INTERVAL 7 DAY);
SET @d30 := DATE_SUB(@now, INTERVAL 30 DAY);

-- Pesquisas
INSERT INTO surveys (titulo, ativo, data_validade, created_at, updated_at, deleted_at)
VALUES ('Satisfação Mobile', TRUE, DATE_ADD(@now, INTERVAL 90 DAY), @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @s1 := LAST_INSERT_ID();

INSERT INTO surveys (titulo, ativo, data_validade, created_at, updated_at, deleted_at)
VALUES ('NPS Clientes 2024', TRUE, DATE_ADD(@now, INTERVAL 120 DAY), @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @s2 := LAST_INSERT_ID();

INSERT INTO surveys (titulo, ativo, data_validade, created_at, updated_at, deleted_at)
VALUES ('Feedback Produto Beta', TRUE, DATE_ADD(@now, INTERVAL 60 DAY), @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @s3 := LAST_INSERT_ID();

-- Perguntas
INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at, deleted_at)
VALUES ('Quão satisfeito você está com o app?', 1, @s1, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @q1a := LAST_INSERT_ID();

INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at, deleted_at)
VALUES ('Qual funcionalidade você mais usa?', 2, @s1, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @q1b := LAST_INSERT_ID();

INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at, deleted_at)
VALUES ('Qual seu NPS hoje?', 1, @s2, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @q2a := LAST_INSERT_ID();

INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at, deleted_at)
VALUES ('O que motivou sua nota?', 2, @s2, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @q2b := LAST_INSERT_ID();

INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at, deleted_at)
VALUES ('O produto beta resolve seu problema?', 1, @s3, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @q3a := LAST_INSERT_ID();

INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at, deleted_at)
VALUES ('Quais melhorias são mais urgentes?', 2, @s3, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @q3b := LAST_INSERT_ID();

-- Opções
-- Satisfação Mobile (q1a)
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Muito satisfeito', TRUE, @q1a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1a1 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Satisfeito', TRUE, @q1a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1a2 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Neutro', TRUE, @q1a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1a3 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Insatisfeito', TRUE, @q1a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1a4 := LAST_INSERT_ID();

-- Funcionalidade (q1b)
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Timeline', TRUE, @q1b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1b1 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Notificações', TRUE, @q1b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1b2 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Chats', TRUE, @q1b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1b3 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Outros', TRUE, @q1b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o1b4 := LAST_INSERT_ID();

-- NPS (q2a)
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Detrator (0-6)', TRUE, @q2a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o2a1 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Neutro (7-8)', TRUE, @q2a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o2a2 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Promotor (9-10)', TRUE, @q2a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o2a3 := LAST_INSERT_ID();

-- Motivo NPS (q2b)
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Preço', TRUE, @q2b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o2b1 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Qualidade', TRUE, @q2b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o2b2 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Atendimento', TRUE, @q2b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o2b3 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Facilidade de uso', TRUE, @q2b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o2b4 := LAST_INSERT_ID();

-- Beta resolve? (q3a)
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Sim, totalmente', TRUE, @q3a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o3a1 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Parcialmente', TRUE, @q3a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o3a2 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Ainda não', TRUE, @q3a, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o3a3 := LAST_INSERT_ID();

-- Melhorias (q3b)
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Performance', TRUE, @q3b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o3b1 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('UX/Fluxos', TRUE, @q3b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o3b2 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Integrações', TRUE, @q3b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o3b3 := LAST_INSERT_ID();
INSERT INTO options (texto, ativo, question_id, created_at, updated_at, deleted_at)
VALUES ('Suporte', TRUE, @q3b, @now, @now, NULL)
ON DUPLICATE KEY UPDATE id=LAST_INSERT_ID(id);
SET @o3b4 := LAST_INSERT_ID();

-- Votos (presentes, 7 dias atrás e 30 dias atrás) para preencher dashboards
INSERT INTO votes (survey_id, question_id, option_id, ip_address, user_agent, created_at, response_session_id)
VALUES
-- Satisfação Mobile
(@s1, @q1a, @o1a1, '192.168.0.10', 'Mozilla/5.0', @now, NULL),
(@s1, @q1a, @o1a2, '192.168.0.11', 'Mozilla/5.0', @now, NULL),
(@s1, @q1a, @o1a2, '192.168.0.12', 'Mozilla/5.0', @now, NULL),
(@s1, @q1a, @o1a3, '192.168.0.13', 'Mozilla/5.0', @now, NULL),
(@s1, @q1a, @o1a4, '192.168.0.14', 'Mozilla/5.0', @now, NULL),
(@s1, @q1b, @o1b1, '192.168.0.10', 'Mozilla/5.0', @now, NULL),
(@s1, @q1b, @o1b2, '192.168.0.11', 'Mozilla/5.0', @now, NULL),
(@s1, @q1b, @o1b2, '192.168.0.12', 'Mozilla/5.0', @now, NULL),
(@s1, @q1b, @o1b3, '192.168.0.13', 'Mozilla/5.0', @now, NULL),
(@s1, @q1b, @o1b4, '192.168.0.14', 'Mozilla/5.0', @now, NULL),
(@s1, @q1a, @o1a1, '192.168.0.21', 'Mozilla/5.0 (Android)', @d7, NULL),
(@s1, @q1a, @o1a2, '192.168.0.22', 'Mozilla/5.0 (iOS)', @d30, NULL),
(@s1, @q1a, @o1a3, '192.168.0.23', 'Mozilla/5.0 (Android)', @d7, NULL),
(@s1, @q1b, @o1b2, '192.168.0.24', 'Mozilla/5.0 (iOS)', @d30, NULL),
(@s1, @q1b, @o1b3, '192.168.0.25', 'Mozilla/5.0 (Windows)', @d7, NULL),
(@s1, @q1b, @o1b1, '192.168.0.26', 'Mozilla/5.0 (Mac)', @d30, NULL),
-- NPS
(@s2, @q2a, @o2a3, '10.0.0.1',  'Chrome', @now, NULL),
(@s2, @q2a, @o2a3, '10.0.0.2',  'Chrome', @now, NULL),
(@s2, @q2a, @o2a2, '10.0.0.3',  'Chrome', @now, NULL),
(@s2, @q2a, @o2a1, '10.0.0.4',  'Chrome', @now, NULL),
(@s2, @q2b, @o2b2, '10.0.0.1',  'Chrome', @now, NULL),
(@s2, @q2b, @o2b1, '10.0.0.2',  'Chrome', @now, NULL),
(@s2, @q2b, @o2b3, '10.0.0.3',  'Chrome', @now, NULL),
(@s2, @q2b, @o2b4, '10.0.0.4',  'Chrome', @now, NULL),
(@s2, @q2a, @o2a3, '10.0.0.21', 'Chrome (Android)', @d7, NULL),
(@s2, @q2a, @o2a2, '10.0.0.22', 'Chrome (Windows)', @d30, NULL),
(@s2, @q2a, @o2a1, '10.0.0.23', 'Safari (iOS)', @d7, NULL),
(@s2, @q2b, @o2b1, '10.0.0.24', 'Chrome (Mac)', @d30, NULL),
(@s2, @q2b, @o2b3, '10.0.0.25', 'Edge', @d7, NULL),
(@s2, @q2b, @o2b4, '10.0.0.26', 'Chrome (Linux)', @d30, NULL),
-- Beta
(@s3, @q3a, @o3a1, '172.16.0.10', 'Safari', @now, NULL),
(@s3, @q3a, @o3a2, '172.16.0.11', 'Safari', @now, NULL),
(@s3, @q3a, @o3a2, '172.16.0.12', 'Safari', @now, NULL),
(@s3, @q3a, @o3a3, '172.16.0.13', 'Safari', @now, NULL),
(@s3, @q3b, @o3b1, '172.16.0.10', 'Safari', @now, NULL),
(@s3, @q3b, @o3b2, '172.16.0.11', 'Safari', @now, NULL),
(@s3, @q3b, @o3b3, '172.16.0.12', 'Safari', @now, NULL),
(@s3, @q3b, @o3b4, '172.16.0.13', 'Safari', @now, NULL),
(@s3, @q3a, @o3a1, '172.16.0.21', 'Safari (iOS)', @d7, NULL),
(@s3, @q3a, @o3a2, '172.16.0.22', 'Chrome (Android)', @d30, NULL),
(@s3, @q3a, @o3a3, '172.16.0.23', 'Firefox', @d7, NULL),
(@s3, @q3b, @o3b1, '172.16.0.24', 'Chrome (Windows)', @d30, NULL),
(@s3, @q3b, @o3b2, '172.16.0.25', 'Safari (Mac)', @d7, NULL),
(@s3, @q3b, @o3b4, '172.16.0.26', 'Edge', @d30, NULL)
ON DUPLICATE KEY UPDATE id=id;

COMMIT;
