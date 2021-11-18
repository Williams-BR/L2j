CREATE TABLE IF NOT EXISTS `global_variables` (
  `var`  VARCHAR(255) NOT NULL DEFAULT '',
  `value` VARCHAR(255) ,
  PRIMARY KEY (`var`)
) ENGINE=MyISAM DEFAULT CHARSET=utf8 COLLATE=utf8_unicode_ci;
REPLACE INTO `global_variables` VALUES ('HBLevel', '11');
REPLACE INTO `global_variables` VALUES ('HBTrust', '4000000');
