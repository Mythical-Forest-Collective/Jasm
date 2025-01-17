package me.darknet.assembler.parser;

import me.darknet.assembler.parser.groups.IdentifierGroup;

import java.util.*;

public class ParserContext {

    public List<Group> groups;
    public Queue<Token> tokens;
    public Token currentToken;
    public Parser parser;
    public Map<String, Group[]> macros = new HashMap<>();

    public ParserContext(Queue<Token> tokens, Parser parser) {
        this.groups = new ArrayList<>();
        this.tokens = tokens;
        this.parser = parser;
    }

    /**
     * Parses next group and checks if it is {@param type} if not throws exception
     * @param type type of group
     * @return next group
     */
    public Group nextGroup(Group.GroupType type) throws AssemblerException {
        Group group = parseNext();
        if (group == null) {
            throw new AssemblerException("Unexpected end of file", currentToken.getLocation());
        }
        if (group.type != type) {
            throw new AssemblerException("Expected " + type.name() + " but got " + group.type.name(), group.location());
        }
        return group;
    }

    public Group parseNext() throws AssemblerException {
        return parser.group(this);
    }

    public Token nextToken() {
        currentToken = tokens.poll();
        return currentToken;
    }

    public boolean hasNextToken() {
        return !tokens.isEmpty();
    }

    public void pushGroup(Group group) {
        groups.add(group);
    }

    /**
     * Reads the next token and directly interprets it as an identifier group
     * @return identifier group
     */
    public IdentifierGroup explicitIdentifier() throws AssemblerException {
        if(!hasNextToken()) {
            throw new AssemblerException("Expected identifier", currentToken.getLocation());
        }
        // do still macro expansion
        Token token = nextToken();
        if(macros.containsKey(token.content)) {
            return new IdentifierGroup(macros.get(token.content)[0].value); // dirty hack
        }
        return new IdentifierGroup(token);
    }

    public void pushGroup(Group.GroupType type, Token val, Group... children) {
        groups.add(new Group(type, val, children));
    }

    public void pushGroup(Group.GroupType type, Group... children) {
        pushGroup(type, currentToken, children);
    }

    public Group previousGroup() throws AssemblerException {
        if(groups.size() > 0) {
            return groups.get(groups.size() - 1);
        } else {
            throw new AssemblerException("No previous group", currentToken.getLocation());
        }
    }

    public Token peekToken() throws AssemblerException{
        if(!hasNextToken()) {
            throw new AssemblerException("Unexpected end of file", currentToken.getLocation());
        }
        return tokens.peek();
    }

    /**
     * Silently peek a token and instead of throwing an error just return EOF
     * @return next token or EOF
     */
    public Token silentPeek() {
        if(!hasNextToken()) {
            return new Token("EOF", currentToken.getLocation(), Token.TokenType.EOF);
        }
        return tokens.peek();
    }

    /**
     * Does the final pass over the groups to finalize them and move extends and implements to the correct place
     */
    public void pass() {

    }

    public Collection<Group> parse() throws AssemblerException {
        while (hasNextToken()) {
            Group group = parseNext();
            if (group != null) {
                groups.add(group);
            }
        }
        pass();
        return groups;
    }
}
