SELECT COL.*, COM.COMMENTS
FROM USER_TAB_COLUMNS COL
         INNER JOIN USER_COL_COMMENTS COM
                    ON COL.TABLE_NAME = COM.TABLE_NAME
                        AND COL.COLUMN_NAME = COM.COLUMN_NAME %s
ORDER BY COL.TABLE_NAME, COL.COLUMN_ID