-- Seeds para desenvolvimento/testes manuais

INSERT INTO users (username, password, role, created_at)
VALUES ('admin', '$2a$10$Q/HpkR8doa9PEd2ZB1WvcuW0k5JgDDxhAHeLx3FZHFVjtYJHeKIVq', 'ADMIN', NOW());
-- password: admin123

-- Pesquisa ativa
INSERT INTO surveys (titulo, ativo, data_validade, created_at, updated_at)
VALUES ('Pesquisa Ativa Demo', true, CURRENT_TIMESTAMP + INTERVAL '30' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
SET @survey_active_id = LAST_INSERT_ID();

INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at)
VALUES ('Como você avalia nosso produto?', 1, @survey_active_id, NOW(), NOW()),
       ('Recomendaria para amigos?', 2, @survey_active_id, NOW(), NOW());
SET @q1 = (SELECT id FROM questions WHERE survey_id=@survey_active_id AND ordem=1);
SET @q2 = (SELECT id FROM questions WHERE survey_id=@survey_active_id AND ordem=2);

INSERT INTO options (texto, ativo, question_id, created_at, updated_at)
VALUES ('Excelente', true, @q1, NOW(), NOW()),
       ('Bom', true, @q1, NOW(), NOW()),
       ('Regular', true, @q1, NOW(), NOW()),
       ('Ruim', false, @q1, NOW(), NOW()),
       ('Sim', true, @q2, NOW(), NOW()),
       ('Não', true, @q2, NOW(), NOW());

-- Pesquisa expirada
INSERT INTO surveys (titulo, ativo, data_validade, created_at, updated_at)
VALUES ('Pesquisa Expirada Demo', true, CURRENT_TIMESTAMP - INTERVAL '1' DAY, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
SET @survey_expired_id = LAST_INSERT_ID();

INSERT INTO questions (texto, ordem, survey_id, created_at, updated_at)
VALUES ('Motivo da visita?', 1, @survey_expired_id, NOW(), NOW());
SET @q3 = (SELECT id FROM questions WHERE survey_id=@survey_expired_id AND ordem=1);

INSERT INTO options (texto, ativo, question_id, created_at, updated_at)
VALUES ('Compra', true, @q3, NOW(), NOW()),
       ('Pesquisa de preço', true, @q3, NOW(), NOW()),
       ('Suporte', false, @q3, NOW(), NOW());
