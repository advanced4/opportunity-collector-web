package org.jf.common.exceptions;

public class OppsExceptions {

    public static class UnauthorizedSamException extends Exception{
        public UnauthorizedSamException(String message){
            super(message);
        }
    }

    public static class ApiException extends Exception{
        public ApiException(String message){
            super(message);
        }
    }

    public static class BadSamCfgException extends Exception{
        public BadSamCfgException(String message){
            super(message);
        }
    }

    public static class BadGrantsCfgException extends Exception{
        public BadGrantsCfgException(String message){
            super(message);
        }
    }

    public static class NoOpportunitiesException extends Exception{
        public NoOpportunitiesException(String message){
            super(message);
        }
    }

    // aka not my fault
    public static class TheirEndpointException extends Exception{
        public TheirEndpointException(String message){
            super(message);
        }
    }

    public static class InvalidDateException extends Exception{
        public InvalidDateException(String message){
            super(message);
        }
    }


}
