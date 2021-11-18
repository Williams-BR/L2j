CREATE TABLE IF NOT EXISTS `territories` (
  `territoryId` INT NOT NULL DEFAULT 0,
  `castleId` INT NOT NULL DEFAULT 0,
  `fortId` INT NOT NULL DEFAULT 0,
  `ownedWardIds` varchar(30) NOT NULL DEFAULT '',
  PRIMARY KEY (`territoryId`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;

INSERT IGNORE INTO `territories` VALUES
(81,1,101,'81;'),
(82,2,103,'82;'),
(83,3,104,'83;'),
(84,4,105,'84;'),
(85,5,106,'85;'),
(86,6,108,'86;'),
(87,7,109,'87;'),
(88,8,110,'88;'),
(89,9,111,'89;');