import au.com.bytecode.opencsv.CSVParser
import au.com.bytecode.opencsv.CSVReader
import groovy.transform.Field
import groovy.util.slurpersupport.GPathResult

import java.text.SimpleDateFormat

def xmlRate= new XmlSlurper().parse('https://www.ecb.europa.eu/stats/exchange/eurofxref/html/usd.xml')
@Field
def rates = [:]
xmlRate.DataSet.Series.'*'.each {
    rates[it["@TIME_PERIOD"].text()] = it["@OBS_VALUE"].toDouble()
}

CSVReader csvReader = new CSVReader(new FileReader(args[0]), CSVParser.DEFAULT_SEPARATOR, CSVParser.DEFAULT_QUOTE_CHARACTER, 1)

int i =1
csvReader.readAll().findResults { String[] row ->
    i++
    def date = row[2]

    def currency = row[13]
    if (currency != 'USD')
        return null
    def amount = row[8]
    if (amount == null) {
        System.err.println("Amount is null on line $i")
        return null
    }
    def desc = row[11]
    new Row(
            date: date,
            amountUsd: amount.toDouble(),
            rate: rateForDate(date),
            amountEur: (amount.toDouble() / rateForDate(date)).round(2),
            desc: desc,
            currency: currency
    )
}.sort { it.desc }. each {
    println "${it.date} ${it.amountEur} EUR <- '$it.desc' ${it.amountUsd} ${it.currency}"

}

 double rateForDate(String date) {
     SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd")
     while (rates[date] == null) {
        // System.err.println("Cannot find rate for date $date")
         def d = sdf.parse(date)
         date = sdf.format(d.minus(1))
     }
    rates[date].toDouble()
}

class Row {
    String date
    double amountUsd
    double amountEur
    double rate
    String desc
    String currency

    SimpleDateFormat sdf_to = new SimpleDateFormat("dd.MM.yy")
    SimpleDateFormat sdf_from = new SimpleDateFormat("yyyy-MM-dd")
    String getDate() {
        return sdf_to.format(sdf_from.parse(this.@date))
    }
}