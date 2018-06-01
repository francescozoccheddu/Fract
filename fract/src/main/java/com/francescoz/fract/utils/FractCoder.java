package com.francescoz.fract.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public final class FractCoder {

    private static final String OPENING = "fractencoderv1";
    private final Node root;

    public FractCoder() {
        root = new Node();
    }

    public void parseAndReplace(String filepath) throws IOException {
        parseAndReplace(new File(filepath));
    }

    public void parseAndMerge(String filepath) throws IOException {
        parseAndMerge(new File(filepath));
    }

    public void parseAndReplace(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        parseAndReplace(bufferedReader);
        bufferedReader.close();
    }

    public void parseAndMerge(File file) throws IOException {
        BufferedReader bufferedReader = new BufferedReader(new FileReader(file));
        parseAndMerge(bufferedReader);
        bufferedReader.close();
    }

    public void parseAndReplace(BufferedReader bufferedReader) throws IOException {
        root.clear();
        parseAndMerge(bufferedReader);
    }

    public void parseAndMerge(BufferedReader bufferedReader) throws IOException {
        String line = bufferedReader.readLine();
        if (line != null && line.equals(OPENING))
            root.parse(bufferedReader);
        else
            throw new RuntimeException("Bad formatted file");
    }

    public Node getNodeRoot() {
        return root;
    }

    public void write(String filepath) throws IOException {
        write(new File(filepath));
    }

    public void write(File file) throws IOException {
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        write(bufferedWriter);
        bufferedWriter.close();
    }

    public void write(BufferedWriter bufferedWriter) throws IOException {
        bufferedWriter.write(OPENING);
        bufferedWriter.newLine();
        root.write(bufferedWriter);
        bufferedWriter.write(Node.Tags.END);
    }

    public interface Decoder<T extends Encodable> {

        T decode(Node node);
    }

    public interface Coder<T> {
        T decode(Node node);

        Node encode(T encodable);
    }

    public interface Encodable {
        Node encode();
    }

    public interface Codable extends Encodable {
        void decode(Node node);
    }

    public static final class Node {

        public final Data<Boolean> booleanData = new PrimitiveData<Boolean>(Tags.BOOLEAN) {
            @Override
            protected Boolean valueOf(String string) {
                return Boolean.parseBoolean(string);
            }
        };
        public final Data<Integer> integerData = new PrimitiveData<Integer>(Tags.INTEGER) {
            @Override
            protected Integer valueOf(String string) {
                return Integer.parseInt(string);
            }
        };
        public final Data<Float> floatData = new PrimitiveData<Float>(Tags.FLOAT) {
            @Override
            protected Float valueOf(String string) {
                return Float.parseFloat(string);
            }
        };
        public final Data<String> stringData = new PrimitiveData<String>(Tags.STRING) {
            @Override
            protected String valueOf(String string) {
                return string;
            }
        };
        public final Data<Node> nodeData = new Data<Node>(Tags.NODE) {

            @Override
            protected void write(BufferedWriter bufferedWriter, Map.Entry<String, Node> entry) throws IOException {
                bufferedWriter.write(Tags.NODE + Tags.SEPARATOR + entry.getKey());
                bufferedWriter.newLine();
                entry.getValue().write(bufferedWriter);
                bufferedWriter.write(Tags.END);
                bufferedWriter.newLine();
            }

            @Override
            protected void parse(BufferedReader bufferedReader) throws IOException {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] tokens = line.split(Tags.SEPARATOR, 2);
                    if (tokens.length == 1 && tokens[0].equals(Tags.END))
                        return;
                    else if (tokens.length == 2) {
                        if (!tokens[0].equals(tag))
                            throw new RuntimeException("Wrong statement");
                        Node n = new Node();
                        put(tokens[1], n);
                        n.parse(bufferedReader);
                    } else
                        throw new RuntimeException("Wrong statement");
                }
                throw new RuntimeException("Unexpected end of file");
            }
        };
        private final Data[] datas = new Data[]{
                nodeData, booleanData, integerData, floatData, stringData
        };

        private void write(BufferedWriter bufferedWriter) throws IOException {
            for (Data d : datas)
                d.write(bufferedWriter);
        }

        private void parse(BufferedReader bufferedReader) throws IOException {
            String line;
            while ((line = bufferedReader.readLine()) != null) {
                String[] tokens = line.split(Tags.SEPARATOR, 2);
                if (tokens.length == 1) {
                    String t = tokens[0];
                    if (t.equals(Tags.END))
                        return;
                    for (Data d : datas) {
                        if (d.isOpeningTag(t)) {
                            d.parse(bufferedReader);
                            break;
                        }
                    }
                } else
                    throw new RuntimeException("Wrong statement");
            }
            throw new RuntimeException("Unexpected end of file");
        }

        public void clear() {
            for (Data d : datas)
                d.removeAll();
        }

        public <T extends Codable> T decodeOrGetEncodable(String key, Decoder<T> decoder, T codable, T defaultValue) {
            if (nodeData.contains(key)) {
                if (codable != null) {
                    decode(key, codable);
                    return codable;
                }
                return getEncodable(key, decoder);
            }
            return defaultValue;
        }

        public <T extends Codable> T decodeOrGetEncodable(String key, Decoder<T> decoder, T codable) {
            return decodeOrGetEncodable(key, decoder, codable, null);
        }

        public <T extends Encodable> T getEncodableIf(String key, Decoder<T> decoder) {
            return getEncodableIf(key, decoder, null);
        }

        public <T extends Encodable> T getEncodableIf(String key, Decoder<T> decoder, T defaultValue) {
            if (nodeData.contains(key))
                return getEncodable(key, decoder);
            return defaultValue;
        }

        public <T extends Encodable> T getEncodable(String key, Decoder<T> decoder) {
            return decoder.decode(nodeData.get(key));
        }

        public void decodeIf(String key, Codable codable) {
            if (codable != null && nodeData.contains(key))
                decode(key, codable);
        }

        public void decode(String key, Codable codable) {
            codable.decode(nodeData.get(key));
        }

        public void putEncodableIf(String key, Encodable encodable) {
            if (encodable == null) return;
            putEncodable(key, encodable);
        }

        public void putEncodable(String key, Encodable encodable) {
            nodeData.put(key, encodable.encode());
        }

        private interface Tags {
            String BOOLEAN = "bool";
            String STRING = "string";
            String INTEGER = "int";
            String FLOAT = "float";
            String NODE = "node";
            String END = "end";
            String SEPARATOR = " ";
        }

        private static abstract class PrimitiveData<T> extends Data<T> {

            private PrimitiveData(String tag) {
                super(tag);
            }

            @Override
            protected void write(BufferedWriter bufferedWriter, Map.Entry<String, T> entry) throws IOException {
                bufferedWriter.write(entry.getKey() + Tags.SEPARATOR + entry.getValue());
                bufferedWriter.newLine();
            }

            protected abstract T valueOf(String string);

            @Override
            protected void parse(BufferedReader bufferedReader) throws IOException {
                String line;
                while ((line = bufferedReader.readLine()) != null) {
                    String[] tokens = line.split(Tags.SEPARATOR, 2);
                    if (tokens.length == 1 && tokens[0].equals(Tags.END))
                        return;
                    else if (tokens.length == 2)
                        put(tokens[0], valueOf(tokens[1]));
                    else
                        throw new RuntimeException("Wrong statement");
                }
                throw new RuntimeException("Unexpected end of file");
            }
        }

        public static abstract class Data<T> {
            protected final String tag;
            private final Map<String, T> map;

            private Data(String tag) {
                map = new HashMap<>(0);
                this.tag = tag;
            }

            public Set<Map.Entry<String, T>> getEntrySet() {
                return map.entrySet();
            }

            public Set<String> getKeySet() {
                return map.keySet();
            }

            public Collection<T> getValuesSet() {
                return map.values();
            }

            public final T get(String key) {
                return map.get(key);
            }

            public final T get(String key, T defaultValue) {
                T t = map.get(key);
                return t == null ? defaultValue : t;
            }

            public final boolean contains(String key) {
                return map.containsKey(key);
            }

            protected void write(BufferedWriter bufferedWriter) throws IOException {
                if (map.isEmpty()) return;
                bufferedWriter.write(tag);
                bufferedWriter.newLine();
                Set<Map.Entry<String, T>> entrySet = map.entrySet();
                for (Map.Entry<String, T> entry : entrySet) {
                    write(bufferedWriter, entry);
                }
                bufferedWriter.write(Tags.END);
                bufferedWriter.newLine();
            }

            protected abstract void write(BufferedWriter bufferedWriter, Map.Entry<String, T> entry) throws IOException;

            public final void put(String key, T value) {
                if (key == null)
                    throw new RuntimeException("Null data key");
                if (value == null)
                    throw new RuntimeException("Null data value");
                map.put(key, value);
            }


            private final boolean isOpeningTag(String token) {
                return token.equals(tag);
            }

            protected abstract void parse(BufferedReader bufferedReader) throws IOException;

            public final boolean remove(String key) {
                return map.remove(key) != null;
            }

            public final void removeAll() {
                map.clear();
            }

        }

    }

}