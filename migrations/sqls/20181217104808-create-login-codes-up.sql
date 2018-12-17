/* Replace with your SQL commands */

CREATE OR REPLACE FUNCTION AlphaNumericSerial() 
RETURNS char(16) AS $$
DECLARE _serial char(16); _i int; _chars char(36) = 'abcdefghijklmnopqrstuvwxyz0123456789';
BEGIN
    _serial = '';
    FOR _i in 1 .. 16 LOOP
        _serial = _serial || substr(_chars, int4(floor(random() * length(_chars))) + 1, 1);
    END LOOP;
    RETURN lower(_serial);
END;
$$ LANGUAGE plpgsql VOLATILE;


CREATE TABLE login_codes (
       id SERIAL PRIMARY KEY,
       account_id INTEGER REFERENCES accounts(id) ON DELETE CASCADE,
       code CHAR(16) DEFAULT AlphaNumericSerial() UNIQUE,
       is_spent BOOLEAN DEFAULT FALSE,
       created_at TIMESTAMP DEFAULT NOW(),
       updated_at TIMESTAMP DEFAULT NOW()
);

CREATE INDEX login_code ON login_codes (code);

CREATE TRIGGER update_login_codes_updated_at
BEFORE UPDATE
ON login_codes
FOR EACH ROW
EXECUTE PROCEDURE update_updated_at_column();
