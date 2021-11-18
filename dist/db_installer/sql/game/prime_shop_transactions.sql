CREATE TABLE IF NOT EXISTS `prime_shop_transactions` (
  `charId` INT UNSIGNED NOT NULL DEFAULT 0,
  `productId` INT NOT NULL DEFAULT 0,
  `quantity` INT NOT NULL DEFAULT 1,
  `transactionTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=MyISAM DEFAULT CHARSET=latin1 COLLATE=latin1_general_ci;