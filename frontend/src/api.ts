import axios from "axios";

const KITE_API = axios.create({
  baseURL: "http://localhost:8080/api/kite",
});

export default KITE_API;
