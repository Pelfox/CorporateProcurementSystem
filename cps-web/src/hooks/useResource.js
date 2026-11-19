import { useEffect, useState } from 'react';
export default function useResource(loader) {
    const [state, setState] = useState({ data: null, loading: true, error: '' });
    const [revision, setRevision] = useState(0);
    useEffect(() => {
        let active = true;
        setState((previous) => ({ ...previous, loading: true, error: '' }));
        Promise.resolve().then(loader).then((data) => { if (active) setState({ data, loading: false, error: '' }); })
            .catch((error) => { if (active) setState((previous) => ({ ...previous, loading: false, error: error.message || 'Не удалось загрузить данные' })); });
        return () => { active = false; };
    }, [loader, revision]);
    return { ...state, reload: () => setRevision((value) => value + 1) };
}
