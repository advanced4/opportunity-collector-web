package org.jf.common.models;

import com.google.gson.JsonElement;

public class Response {

    public enum Status {
        ok, error
    }

    public abstract static class BaseResponse {
        private transient String msg;
        private transient Status status;

        abstract public String getMsg();
        abstract public void setMsg(String msg);

        abstract public Status getStatus();
        abstract public void setStatus(Status status);
    }

    public static class StandardResponse extends BaseResponse{
        private Status status;
        private String msg;

        public StandardResponse(){
            this.status = Status.error;
        }

        public String getMsg() {return msg;}
        public void setMsg(String msg) {this.msg = msg;}

        public Status getStatus() {return status;}
        public void setStatus(Status status) {this.status = status;}
    }

    public static class StandardDataResponse extends BaseResponse{
        private Status status;
        private String msg;
        private JsonElement data;

        public StandardDataResponse(){
            this.status = Status.error;
        }

        public JsonElement getData() {return data;}
        public void setData(JsonElement data) {this.data = data;}

        public String getMsg() {return msg;}
        public void setMsg(String msg) {this.msg = msg;}

        public Status getStatus() {return status;}
        public void setStatus(Status status) {this.status = status;}
    }

}
