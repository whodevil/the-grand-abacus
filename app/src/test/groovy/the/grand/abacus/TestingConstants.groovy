package the.grand.abacus

class TestingConstants {

    static final String FILE_DATE = "2022-01"
    static final String ACCOUNT_A = "12345678"
    static final String ACCOUNT_B = "98765432"
    static final String BALANCE = "1000000"
    static final String CREDIT_NAME = "Credit NAME"
    static final String CREDIT_AMOUNT = "2.82"
    static final String TRANSFER_AMOUNT = "-200.00"
    static final String PAYPAL_AMOUNT = "-62.99"

    static final String OFX_DATA = """
OFXHEADER:100
DATA:OFXSGML
VERSION:102
SECURITY:NONE
ENCODING:USASCII
CHARSET:1252
COMPRESSION:NONE
OLDFILEUID:NONE
NEWFILEUID:NONE

<OFX>
\t<SIGNONMSGSRSV1>
\t\t<SONRS>
\t\t\t<STATUS>
\t\t\t\t<CODE>0
\t\t\t\t<SEVERITY>INFO
\t\t\t</STATUS>
\t\t\t<DTSERVER>20220219004351.658
\t\t\t<LANGUAGE>ENG
\t\t\t<FI>
\t\t\t\t<ORG>FAKE BANK
\t\t\t\t<FID>9999
\t\t\t</FI>
\t\t</SONRS>
\t</SIGNONMSGSRSV1>

\t<BANKMSGSRSV1>
\t\t<STMTTRNRS>
\t\t\t<TRNUID>1
\t\t\t<STATUS>
\t\t\t\t<CODE>0
\t\t\t\t<SEVERITY>INFO
\t\t\t</STATUS>
\t\t\t<STMTRS>
\t\t\t\t<CURDEF>USD
\t\t\t\t<BANKACCTFROM>
\t\t\t\t\t<BANKID>USA
\t\t\t\t\t<ACCTID>${ACCOUNT_A}
\t\t\t\t\t<ACCTTYPE>CHECKING
\t\t\t\t</BANKACCTFROM>

\t\t\t\t<BANKTRANLIST>
\t\t\t\t\t<DTSTART>20220101
\t\t\t\t\t<DTEND>20220131
\t\t\t\t\t<STMTTRN>
\t\t\t\t\t\t<TRNTYPE>CREDIT
\t\t\t\t\t\t<DTPOSTED>20220131000000.000[-08:PST]
\t\t\t\t\t\t<TRNAMT>${CREDIT_AMOUNT}

\t\t\t\t\t\t<FITID>12345
\t\t\t\t\t\t<NAME>Credit NAME
\t\t\t\t\t\t<MEMO>Credit MEMO
\t\t\t\t\t</STMTTRN>
\t\t\t\t\t<STMTTRN>
\t\t\t\t\t\t<TRNTYPE>DEBIT
\t\t\t\t\t\t<DTPOSTED>20220101000000.000[-08:PST]
\t\t\t\t\t\t<TRNAMT>${TRANSFER_AMOUNT}

\t\t\t\t\t\t<FITID>1234567
\t\t\t\t\t\t<NAME>Withdrawal Transfer To ${ACCOUNT_B}
\t\t\t\t\t\t<MEMO>Withdrawal Transfer To ${ACCOUNT_B}
\t\t\t\t\t</STMTTRN>
\t\t\t\t\t<STMTTRN>
\t\t\t\t\t\t<TRNTYPE>DEBIT
\t\t\t\t\t\t<DTPOSTED>20220131000000.000[-08:PST]
\t\t\t\t\t\t<TRNAMT>${PAYPAL_AMOUNT}

\t\t\t\t\t\t<FITID>4325
\t\t\t\t\t\t<NAME>ACH Debit PAYPAL INSTANT TRANSFE
\t\t\t\t\t\t<MEMO>ACH Debit PAYPAL INSTANT TRANSFER  INST XFER 
\t\t\t\t\t</STMTTRN>
\t\t\t\t\t<STMTTRN>
\t\t\t\t\t\t<TRNTYPE>DEBIT
\t\t\t\t\t\t<DTPOSTED>20220124000000.000[-08:PST]
\t\t\t\t\t\t<TRNAMT>-7.59

\t\t\t\t\t\t<FITID>20220124 1989305 759 202,201,245,239
\t\t\t\t\t\t<NAME>POS Transaction  AMAZONCOM      
\t\t\t\t\t\t<MEMO>POS Transaction  AMAZONCOM             SEATTLE      WAUS
\t\t\t\t\t</STMTTRN>
\t\t\t\t</BANKTRANLIST>
\t\t\t\t<LEDGERBAL>
\t\t\t\t<BALAMT>${BALANCE}</BALAMT>
\t\t\t\t<DTASOF>20220219004351.658</DTASOF>
\t\t\t\t</LEDGERBAL>

\t\t\t\t<AVAILBAL>
\t\t\t\t<BALAMT>${BALANCE}</BALAMT>
\t\t\t\t<DTASOF>20220219004351.658</DTASOF>
\t\t\t\t</AVAILBAL>

\t\t\t</STMTRS>
\t\t</STMTTRNRS>
\t</BANKMSGSRSV1>
</OFX>

"""

    static final String PAYPAL_CSV = """
"Date","Time","TimeZone","Name","Type","Status","Currency","Amount","Receipt ID","Balance"
"01/02/2022","15:46:58","PST","GrubHub Seamless","General Authorization","Completed","USD","-52.62","","5.00"
"01/03/2022","09:01:46","PST","Lyft","PreApproved Payment Bill User Payment","Completed","USD","-23.20","","-18.20"
"01/03/2022","20:05:37","PST","Instacart","General Authorization","Completed","USD","-215.00","","5.00"
"01/30/2022","07:23:53","PST","Adobe, Inc.","General Authorization","Completed","USD","-62.99","","5.00"
"""
}
