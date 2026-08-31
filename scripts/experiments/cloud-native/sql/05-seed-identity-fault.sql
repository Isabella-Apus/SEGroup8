USE identity_governance_db;

INSERT INTO `user` (
  id, username, password, nickname, role, status,
  credit_score, buyer_credit_score, seller_credit_score
) VALUES
  (2, 'experiment-seller', '$2a$10$unusedExperimentPasswordHash000000000000000000000000',
   'experiment-seller', 'USER', 'NORMAL', 100, 100, 100),
  (3, 'experiment-buyer', '$2a$10$unusedExperimentPasswordHash000000000000000000000000',
   'experiment-buyer', 'USER', 'NORMAL', 100, 100, 100)
ON DUPLICATE KEY UPDATE nickname=VALUES(nickname),status='NORMAL';

INSERT INTO address (
  id, user_id, receiver_name, receiver_phone, province, city, detail_address, is_default
) VALUES (
  1, 3, 'Experiment Buyer', '13800008000', 'Zhejiang', 'Hangzhou',
  'West Lake Road 1', 1
) ON DUPLICATE KEY UPDATE
  user_id=VALUES(user_id),receiver_name=VALUES(receiver_name),receiver_phone=VALUES(receiver_phone),
  province=VALUES(province),city=VALUES(city),detail_address=VALUES(detail_address),is_default=1;
