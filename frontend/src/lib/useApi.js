import { useCallback, useEffect, useState } from "react";
import api, { getErrorMessage } from "../api/client";

export function useApi(path, params) {
  const [data, setData] = useState(null);
  const [loading, setLoading] = useState(Boolean(path));
  const [error, setError] = useState("");
  const paramsKey = JSON.stringify(params || {});

  const load = useCallback(async () => {
    if (!path) {
      setLoading(false);
      return;
    }
    setLoading(true);
    setError("");
    try {
      const response = await api.get(path, { params: params || {} });
      setData(response.data);
    } catch (err) {
      setError(getErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }, [path, paramsKey]);

  useEffect(() => {
    load();
  }, [load]);

  return { data, loading, error, reload: load, setData };
}
