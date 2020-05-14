package org.jf.common.constants;

import java.util.HashMap;
import java.util.Map;

public class OppsConstants {

    public static class Misc{
        public static long ms_in_year = 86400L * 365L * 1000L;
    }

    public static class Jwt{
        // used for JWT Tokens
        public static final String TOKEN_TYPE_JWT = "JWT";
        public static final String KEY_FACTORY_INSTANCE_TYPE = "EC"; // DH, DSA, RSA, EC. Must match algorithm above
        public static final String JwtCookieNameOpps = "opps";

        // These are claim names that we use
        public static final String JwtUid = "i";
        public static final String JwtUsername = "u";
        public static final String JwtAdmin = "a";
    }

    public static class Sam{
        public static String endpoint = "https://api.sam.gov/prod/opportunities/v1/search";
        public static Map<String, String> types_plain_abv = new HashMap<String, String>() {
            {
                put("Justification", "u");
                put("Pre-Solicitation", "p");
                put("Award Notice", "a");
                put("Sources Sought", "r");
                put("Special Notice", "s");
                put("Surplus Property", "g");
                put("Combined Synopsis/Solicitation", "k");
                put("Solicitation", "o");
                put("Intent to Bundle", "i");
            }
        };

        // each one of these has a version with the k,v swapped. both of these are useful for
        // converting between each other
        public static Map<String, String> types_abv_plain = new HashMap<>();
        static {
            {
                for(Map.Entry<String, String> entry : types_plain_abv.entrySet()){
                    types_abv_plain.put(entry.getValue(), entry.getKey());
                }
            }
        };
    }

    public static class Grants{
        public static String endpoint = "https://www.grants.gov/grantsws/rest/opportunities/search/csv/download?osjp=";
        public static Map<String, String> instruments_plain_abv = new HashMap<String, String>() {
            {
                put("Grant", "G");
                put("Cooperative Agreement", "CA");
                put("Procurement Contract", "PC");
                put("Other", "O");
            }
        };

        // each one of these has a version with the k,v swapped. both of these are useful for
        // converting between each other
        public static Map<String, String> instruments_abv_plain = new HashMap<>();
        static {
            {
                for(Map.Entry<String, String> entry : instruments_plain_abv.entrySet()){
                    instruments_abv_plain.put(entry.getValue(), entry.getKey());
                }
            }
        };

        public static Map<String, String> eligibilities_plain_abv = new HashMap<String, String>() {
            {
                put("Unrestricted", "99");
                put("Nonprofits 501C3", "12");
                put("Nonprofits non 501C3", "13");
                put("Private institutions of higher education", "20");
                put("Individuals", "21");
                put("For-profit organizations other than SB", "22");
                put("Small businesses", "23");
                put("Others", "25");

            }
        };

        public static Map<String, String> eligibilities_abv_plain = new HashMap<>();
        static {
            {
                for(Map.Entry<String, String> entry : eligibilities_plain_abv.entrySet()){
                    eligibilities_abv_plain.put(entry.getValue(), entry.getKey());
                }
            }
        };

        public static Map<String, String> cats_plain_abv = new HashMap<String, String>() {
            {
                put("Business & Commerce", "BC");
                put("Community Development", "CD");
                put("Consumer Protection", "CP");
                put("Disaster Prevention & Relief", "DPR");
                put("Education", "ED");
                put("Employment, Labor and Training", "ELT");
                put("Energy", "EN");
                put("Environment", "ENV");
                put("Health", "HL");
                put("Humanities", "HU");
                put("Information and Statistics", "IS");
                put("Law, Justice and Legal Services", "LJL");
                put("Natural Resources", "NR");
                put("Recovery Act", "RA");
                put("Regional Development", "RD");
                put("Science & Technology and other R&D", "ST");
                put("Transportation", "T");
                put("Other", "O");
                put("Affordable Care Act", "ACA");
                put("Agriculture", "AG");
                put("Arts", "AR");
                put("Food & Nutrition", "FN");
                put("Housing", "HO");
                put("Income Security & Social Services", "ISS");
            }
        };
        public static Map<String, String> cats_abv_plain = new HashMap<>();
        static {
            {
                for(Map.Entry<String, String> entry : cats_plain_abv.entrySet()){
                    cats_abv_plain.put(entry.getValue(), entry.getKey());
                }
            }
        };
    }

    public static class LoginSec{
        // when checking for a brute force, we look at the past 2 hours
        public static final int bruteForceLookbackTimeSeconds = 2 * 60 * 60; // 60 seconds * 60 minutes * 2

        // number of failed attempts allowed within time_left_before_token_refresh_ms
        public static final int failedloginAttemptLimit = 5;

        // aka if there's less then 10 minutes left on the token, we'll refresh it
        public static final int time_left_before_token_refresh_ms = 1000 * 60 * 10; // 10 minutes
    }

    public static class WebStuffs{
        public static final String AUTH_HEADER = "Authorization";
        public static final String ACCESS_CONTROL_ALLOW_HEADERS = "Access-Control-Allow-Origin, Content-Disposition, Access-Control-Allow-Headers, Content-Type, Access-Control-Allow-Headers, Authorization";
    }

    // helps avoid mispellings / helps do standard responses
    public static class Strings {
        public static final String OK = "ok";
        public static final String ERROR = "error";
        public static final String MSG = "msg";
        public static final String STATUS = "status";

        // Errors
        public static final String ERR_MISSING_PARAMS = "Missing Parameters";
        public static final String ERR_BAD_LOGIN = "Invalid email or password";
        public static final String ERR_UNAUTHORIZED = "Unauthorized";
        public static final String ERR_INVALID_PARAMS = "Invalid Parameter[s]";
    }

    public enum HttpMethods{
        GET,
        HEAD,
        POST,
        PUT,
        DELETE,
        CONNECT,
        OPTIONS,
        TRACE,
        PATCH
    }
}