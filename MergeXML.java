import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class MergeXML {
    private static final String HEADER_ROW = "NIPR,LicenseID,Jurisdiction,Resident,LicenseClass,EffectiveDate,ExpiryDate,Status,Line,LineEffectiveDate,LineExpiryDate,LineStatus";
   
    private static List<String> validLicenses;
    private static List<String> invalidLicenses;

    public static void main(String[] args) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            Document document1 = builder.parse(new File("License1.xml"));
            Document document2 = builder.parse(new File("License2.xml"));

            document1.getDocumentElement().normalize();
            document2.getDocumentElement().normalize();

            validLicenses = new ArrayList<>();
            invalidLicenses = new ArrayList<>();
            extractLicenseElements(document1);
            extractLicenseElements(document2);

            BufferedWriter mergedFileWriter = new BufferedWriter(new FileWriter("mergedList.txt"));
            BufferedWriter validLicenseWriter = new BufferedWriter(new FileWriter("validLicenses.txt"));
            BufferedWriter invalidLicenseWriter = new BufferedWriter(new FileWriter("invalidLicenses.txt"));

            // Writing headers
            mergedFileWriter.write(HEADER_ROW);
            validLicenseWriter.write(HEADER_ROW);
            invalidLicenseWriter.write(HEADER_ROW);

            // Writing licenses to respective files
            writeLicensesToFile(validLicenses, mergedFileWriter, validLicenseWriter);
            writeLicensesToFile(invalidLicenses, mergedFileWriter, invalidLicenseWriter);

            // Closing writers
            mergedFileWriter.close();
            validLicenseWriter.close();
            invalidLicenseWriter.close();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
          }
    }

    // Method to write licenses to files
    private static void writeLicensesToFile(List<String> licenses, BufferedWriter mergedWriter, BufferedWriter specificWriter) throws IOException {
        for (String license : licenses) {
            mergedWriter.newLine();
            mergedWriter.write(license);

            specificWriter.newLine();
            specificWriter.write(license);
         }
     }

    // Method to extract license elements from an XML document
    public static void extractLicenseElements(Document document) {
        NodeList producerList = document.getElementsByTagName("CSR_Producer");
        List<String> niprs = new ArrayList<>();

        for (int i = 0; i < producerList.getLength(); i++) {
            Element producer = (Element) producerList.item(i);
            String nipr = producer.getAttribute("NIPR_Number");
            int count = producer.getChildNodes().getLength();
 
             for (int j = 0; j < count; j++) {
                niprs.add(nipr);
             }
        }
 
         NodeList licenseList = document.getElementsByTagName("License");
        NodeList loaList = document.getElementsByTagName("LOA");
        int loaLength = loaList.getLength();
        int count = 0;

        for (int i = 0; i < licenseList.getLength(); i++) {
            Element license = (Element) licenseList.item(i);
             String licenseInfo = constructLicenseInfo(niprs, i, license);
             String licenseExpirationDate = license.getAttribute("License_Expiration_Date");

             while (count < loaLength) {
                 Element loa = (Element) loaList.item(count);
                 if (loa == null) {
                    break;
                }

                String loaInfo = licenseInfo;
                String licenseLine = loa.getAttribute("LOA_Name");
                loaInfo += licenseLine + ", ";

                String licenseLineEffectiveDate = loa.getAttribute("LOA_Issue_Date");
                loaInfo += licenseLineEffectiveDate + ", ";

                loaInfo += licenseExpirationDate + ", ";

                 String licenseLineStatus = loa.getAttribute("LOA_Status");
                loaInfo += licenseLineStatus;
 
                 if (isLicenseValid(license)) {
                    validLicenses.add(loaInfo);
                } else {
                    invalidLicenses.add(loaInfo);
                }
                count++;
            }
        }
    }

    // Method to construct a license string with comma separation
     private static String constructLicenseInfo(List<String> niprs, int index, Element license) {
        String licenseInfo = "";
        licenseInfo += niprs.get(index) + ", ";
   
        String licenseId = license.getAttribute("License_Number");
         licenseInfo += licenseId + ", ";
 
         String jurisdiction = license.getAttribute("State_Code");
        licenseInfo += jurisdiction + ", ";

         String resident = license.getAttribute("Resident_Indicator");
        licenseInfo += resident + ", ";

         String licenseClass = license.getAttribute("License_Class");
        licenseInfo += licenseClass + ", ";

         String licenseEffectiveDate = license.getAttribute("Date_Status_Effective");
        licenseInfo += licenseEffectiveDate + ", ";
 
        String licenseExpiryDate = license.getAttribute("License_Expiration_Date");
         licenseInfo += licenseExpiryDate + ", ";

        String licenseStatus = license.getAttribute("License_Status");
         licenseInfo += licenseStatus + ", ";

         return licenseInfo;
    }

    // Method to verify if a license is valid
    public static boolean isLicenseValid(Element license) {
        LocalDate currentDate = LocalDate.now();
         DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy");

        String expirationDateString = license.getAttribute("License_Expiration_Date");
        LocalDate expirationDate = LocalDate.parse(expirationDateString, formatter);

        return expirationDate.isAfter(currentDate) || expirationDate.isEqual(currentDate);
    }
}
